package com.server.mailbox.repository;

import com.server.mailbox.entity.MailboxItem;
import net.teujaem.jpalib.jpa.JpaManager;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 우편함 데이터 접근 계층.
 *
 * SP-Framework가 제공하는 단순화된 {@code DataBase} API(save/find/delete)는
 * 기본키로만 조회가 가능하므로, "특정 플레이어의 모든 우편물"처럼 조건이 필요한
 * 조회/삭제는 SP-Framework가 실제로 내부에서 사용하는 {@link JpaManager}를
 * 직접 사용해 JPQL로 처리합니다. (createJpaManager가 이미 초기화까지 수행합니다.)
 */
public class MailboxRepository {

    private final JpaManager jpaManager;

    public MailboxRepository(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    /** 특정 플레이어의 우편함 아이템을 등록된 순서대로 반환합니다. */
    public List<MailboxItem> findByOwner(UUID owner) {
        return jpaManager.execute(em -> em.createQuery(
                        "SELECT m FROM MailboxItem m WHERE m.ownerUuid = :owner ORDER BY m.id ASC",
                        MailboxItem.class)
                .setParameter("owner", owner.toString())
                .getResultList());
    }

    /** 우편함에 아이템 한 건을 저장합니다. */
    public MailboxItem addItem(MailboxItem item) {
        return jpaManager.save(item);
    }

    /** id로 우편물 한 건을 삭제합니다. 삭제되었으면 true. */
    public boolean removeById(Long id) {
        return jpaManager.execute(em -> {
            MailboxItem found = em.find(MailboxItem.class, id);
            if (found == null) {
                return false;
            }
            em.remove(found);
            return true;
        });
    }

    /** 해당 플레이어의 우편함에서 matcher와 일치하는 첫 번째 아이템 한 건만 삭제합니다. */
    public boolean removeFirstMatching(UUID owner, Predicate<MailboxItem> matcher) {
        return jpaManager.execute(em -> {
            List<MailboxItem> items = em.createQuery(
                            "SELECT m FROM MailboxItem m WHERE m.ownerUuid = :owner ORDER BY m.id ASC",
                            MailboxItem.class)
                    .setParameter("owner", owner.toString())
                    .getResultList();
            for (MailboxItem item : items) {
                if (matcher.test(item)) {
                    em.remove(item);
                    return true;
                }
            }
            return false;
        });
    }

    /** 특정 플레이어의 우편함을 전부 비웁니다. */
    public void deleteAllForOwner(UUID owner) {
        jpaManager.execute(em -> em.createQuery("DELETE FROM MailboxItem m WHERE m.ownerUuid = :owner")
                .setParameter("owner", owner.toString())
                .executeUpdate());
    }

    /** 모든 플레이어의 우편함을 전부 비웁니다. */
    public void deleteAll() {
        jpaManager.execute(em -> em.createQuery("DELETE FROM MailboxItem m").executeUpdate());
    }
}
