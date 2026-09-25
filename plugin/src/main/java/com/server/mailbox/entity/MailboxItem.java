package com.server.mailbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 우편함에 들어있는 아이템 한 건을 나타내는 JPA 엔티티.
 * SP-Framework의 DatabaseController가 이 클래스를 스캔해 테이블을 자동 생성/갱신합니다(ddl-auto: update).
 */
@Entity
@Table(
        name = "mailbox_items",
        indexes = @Index(name = "idx_mailbox_owner", columnList = "owner_uuid")
)
public class MailboxItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "owner_uuid", nullable = false, length = 36)
    private String ownerUuid;

    /** ItemStack을 직렬화한 Base64 문자열 (ItemSerialization 참고). */
    @Lob
    @Column(name = "item_data", nullable = false)
    private String itemData;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    /** JPA 스펙상 반드시 필요한 기본 생성자. */
    protected MailboxItem() {
    }

    public MailboxItem(String ownerUuid, String itemData) {
        this.ownerUuid = ownerUuid;
        this.itemData = itemData;
        this.createdAt = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public String getItemData() {
        return itemData;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
