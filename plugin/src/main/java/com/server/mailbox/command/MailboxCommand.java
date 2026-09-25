package com.server.mailbox.command;

import com.server.mailbox.SPMailbox;
import com.server.mailbox.entity.MailboxItem;
import com.server.mailbox.gui.MailboxGui;
import com.server.mailbox.proxy.MailboxProxyService;
import com.server.mailbox.repository.MailboxRepository;
import com.server.mailbox.util.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * mailbox.sk의 "command /우편함 [&lt;text&gt;] [&lt;text&gt;]" 트리거를 그대로 포팅한 명령어.
 * <p>
 * /우편함            - 자신의 우편함을 엽니다.
 * /우편함 추가 &lt;player|all&gt;      - (오피 전용) 들고 있는 아이템을 우편함에 추가합니다.
 * /우편함 아이템빼기 &lt;player&gt;     - (오피 전용) 들고 있는 아이템과 같은 아이템을 우편함에서 뺍니다.
 * /우편함 초기화 &lt;player|all&gt;     - (오피 전용) 우편함을 비웁니다.
 */
public class MailboxCommand implements CommandExecutor, TabCompleter {

    private final SPMailbox plugin;

    public MailboxCommand(SPMailbox plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
                return true;
            }
            MailboxGui.open(plugin, player, 1);
            return true;
        }

        if (!(sender instanceof Player player) || !player.isOp()) {
            sender.sendMessage(color("&c[!]&f 당신은 오피가 없으므로 다른 명령어를 입력할 수 없습니다."));
            return true;
        }

        String sub = args[0];
        String targetArg = args.length > 1 ? args[1] : null;
        Player targetPlayer = targetArg != null ? Bukkit.getPlayerExact(targetArg) : null;

        switch (sub) {
            case "추가" -> handleAdd(player, targetArg, targetPlayer);
            case "아이템빼기" -> handleRemove(player, targetPlayer);
            case "초기화" -> handleReset(player, targetArg, targetPlayer);
            default -> player.sendMessage(color("&c[!]&f 올바른 명령어를 입력해주세요."));
        }
        return true;
    }

    private void handleAdd(Player sender, String targetArg, Player targetPlayer) {
        ItemStack tool = sender.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) {
            sender.sendMessage(color("&c[!]&f 손에 아이템을 들어주세요"));
            return;
        }
        String itemName = displayName(tool);

        if (targetPlayer != null) {
            queueAdd(targetPlayer.getUniqueId(), tool.clone());
            sender.sendMessage(color("&a[!]&f &b[&f" + itemName + "&b]&f아이템을 " + targetPlayer.getName() + "님에게 추가했습니다."));
        } else if ("all".equalsIgnoreCase(targetArg)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                queueAdd(online.getUniqueId(), tool.clone());
            }
            sender.sendMessage(color("&a[!]&f &b[&f" + itemName + "&b]&f아이템을 모든유저에게 추가했습니다."));
        } else {
            sender.sendMessage(color("&c[!]&f 플레이어를 입력해주세요"));
        }
    }

    private void queueAdd(UUID ownerUuid, ItemStack item) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MailboxRepository repository = plugin.getRepository();
            repository.addItem(new MailboxItem(ownerUuid.toString(), ItemSerialization.serialize(item)));
            // DB 저장 후, 다른 서버에 접속해 있을 수도 있는 플레이어에게 프록시로 도착 알림 전송
            MailboxProxyService.notifyMailAdded(ownerUuid);
            // 지금 이 서버에 접속해 있다면 바로 안내
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(ownerUuid);
                if (online != null) {
                    online.sendMessage(color("&a[!]&f 새 우편물이 도착했습니다! &b/우편함&f 으로 확인해보세요."));
                }
            });
        });
    }

    private void handleRemove(Player sender, Player targetPlayer) {
        ItemStack tool = sender.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) {
            sender.sendMessage(color("&c[!]&f 손에 아이템을 들어주세요"));
            return;
        }
        if (targetPlayer == null) {
            sender.sendMessage(color("&c[!]&f 플레이어를 입력해주세요"));
            return;
        }

        String itemName = displayName(tool);
        String targetName = targetPlayer.getName();
        UUID targetUuid = targetPlayer.getUniqueId();
        ItemStack toMatch = tool.clone();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean removed = plugin.getRepository().removeFirstMatching(targetUuid,
                    stored -> ItemSerialization.deserialize(stored.getItemData()).isSimilar(toMatch));
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (removed) {
                    sender.sendMessage(color("&a[!]&f &b[&f" + itemName + "&b]&f아이템을 " + targetName + "님에서 뺐습니다."));
                } else {
                    sender.sendMessage(color("&a[!]&f &b[&f" + itemName + "&b]&f아이템은 " + targetName + "님에게 없습니다."));
                }
            });
        });
    }

    private void handleReset(Player sender, String targetArg, Player targetPlayer) {
        if (targetPlayer != null) {
            String targetName = targetPlayer.getName();
            UUID targetUuid = targetPlayer.getUniqueId();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getRepository().deleteAllForOwner(targetUuid);
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color("&a[!]&f " + targetName + "님의 우편함을 초기화 했습니다.")));
            });
        } else if ("all".equalsIgnoreCase(targetArg)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getRepository().deleteAll();
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(color("&a[!]&f 모든유저에 우편함을 초기화 했습니다.")));
            });
        } else {
            sender.sendMessage(color("&c[!]&f 플레이어를 입력해주세요"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.isOp()) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("추가", "아이템빼기", "초기화"), args[0]);
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            names.add("all");
            return filter(names, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }

    private String displayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
