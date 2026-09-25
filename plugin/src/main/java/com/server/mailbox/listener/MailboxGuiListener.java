package com.server.mailbox.listener;

import com.server.mailbox.SPMailbox;
import com.server.mailbox.gui.MailboxGui;
import com.server.mailbox.gui.MailboxHolder;
import com.server.mailbox.repository.MailboxRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mailbox.sk의 "on inventory click" 이벤트를 그대로 포팅했습니다.
 * 아이템 지급 시 중복 클릭으로 인한 복사(듀핑)를 막기 위해 원본과 동일하게
 * 짧은 디바운스(5틱)를 둡니다.
 */
public class MailboxGuiListener implements Listener {

    private final SPMailbox plugin;
    private final Set<UUID> takingItem = ConcurrentHashMap.newKeySet();

    public MailboxGuiListener(SPMailbox plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MailboxHolder mailboxHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);

        if (!event.getInventory().equals(event.getClickedInventory())) {
            // 자신의 하단 인벤토리를 클릭한 경우는 무시 (원본 스크립트와 동일)
            return;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        int page = mailboxHolder.getPage();
        int slot = event.getSlot();

        if (slot == MailboxGui.SLOT_PREV) {
            if (page > 1) {
                MailboxGui.open(plugin, player, page - 1);
            }
            return;
        }
        if (slot == MailboxGui.SLOT_NEXT) {
            MailboxGui.open(plugin, player, page + 1);
            return;
        }
        if (slot >= MailboxGui.PAGE_SIZE) {
            return; // 하단 장식 슬롯
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }

        Long mailId = clicked.getItemMeta().getPersistentDataContainer()
                .get(MailboxGui.mailIdKey(plugin), PersistentDataType.LONG);
        if (mailId == null) {
            return;
        }

        if (!takingItem.add(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[!]&f 잠시 후 다시 시도하세요"));
            return;
        }

        ItemStack toGive = clicked.clone();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MailboxRepository repository = plugin.getRepository();
            boolean removed = repository.removeById(mailId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (removed) {
                    player.getInventory().addItem(toGive).values()
                            .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                }
                MailboxGui.open(plugin, player, page);
                Bukkit.getScheduler().runTaskLater(plugin, () -> takingItem.remove(player.getUniqueId()), 5L);
            });
        });
    }
}
