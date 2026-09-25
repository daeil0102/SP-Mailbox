package net.teujaem.mailbox.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 인벤토리 제목 문자열 비교 대신, 클릭 이벤트에서 "이 인벤토리가 우편함 GUI인지"와
 * "몇 페이지인지"를 안전하게 식별하기 위한 마커용 InventoryHolder.
 */
public class MailboxHolder implements InventoryHolder {

    private final int page;
    private Inventory inventory;

    public MailboxHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
