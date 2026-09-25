package net.teujaem.mailbox.gui;

import net.teujaem.mailbox.SPMailbox;
import net.teujaem.mailbox.entity.MailboxItem;
import net.teujaem.mailbox.repository.MailboxRepository;
import net.teujaem.mailbox.util.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * mailbox.sk의 mailboxOpen 함수를 그대로 포팅한 GUI.
 * 45칸(0~44)은 우편물, 45~53은 하단 장식/페이지 이동 칸입니다.
 */
public final class MailboxGui {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&6[ &2우편함 &6]");
    public static final int PAGE_SIZE = 45;
    public static final int SLOT_PREV = 48;
    public static final int SLOT_PAGE = 49;
    public static final int SLOT_NEXT = 50;

    private MailboxGui() {
    }

    public static NamespacedKey mailIdKey(SPMailbox plugin) {
        return new NamespacedKey(plugin, "mailbox_id");
    }

    /** DB에서 비동기로 목록을 읽어온 뒤, 메인 스레드에서 인벤토리를 열어줍니다. */
    public static void open(SPMailbox plugin, Player player, int page) {
        int safePage = Math.max(page, 1);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MailboxRepository repository = plugin.getRepository();
            List<MailboxItem> items = repository.findByOwner(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> render(plugin, player, safePage, items));
        });
    }

    private static void render(SPMailbox plugin, Player player, int page, List<MailboxItem> items) {
        MailboxHolder holder = new MailboxHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        holder.setInventory(inventory);

        NamespacedKey key = mailIdKey(plugin);
        int startIndex = (page - 1) * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int itemIndex = startIndex + slot;
            if (itemIndex >= items.size()) {
                continue;
            }
            MailboxItem stored = items.get(itemIndex);
            ItemStack display = ItemSerialization.deserialize(stored.getItemData());
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, stored.getId());
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }

        ItemStack glass = namedItem(Material.GRAY_STAINED_GLASS_PANE, "&f");
        for (int slot = PAGE_SIZE; slot < 54; slot++) {
            inventory.setItem(slot, glass);
        }
        inventory.setItem(SLOT_PREV, namedItem(Material.RED_STAINED_GLASS_PANE, "&c전 페이지 (" + (page - 1) + ")"));
        inventory.setItem(SLOT_PAGE, namedItem(Material.WHITE_STAINED_GLASS_PANE, "&b" + page));
        inventory.setItem(SLOT_NEXT, namedItem(Material.GREEN_STAINED_GLASS_PANE, "&2다음 페이지 (" + (page + 1) + ")"));

        player.openInventory(inventory);
    }

    private static ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
