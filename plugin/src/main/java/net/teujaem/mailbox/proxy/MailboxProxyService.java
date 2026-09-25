package net.teujaem.mailbox.proxy;

import net.teujaem.mailbox.SPMailbox;
import net.teujaem.spFramework.SPFramework;
import net.teujaem.spFramework.api.ProxyData;

import java.util.UUID;

/**
 * 우편함 DB는 모든 백엔드 서버가 같은 MariaDB를 바라보므로 이미 서버 간에 공유됩니다.
 * 이 클래스는 "프록시 연동" 부분, 즉 우편물이 도착했을 때 대상 플레이어가
 * 지금 접속해 있는 서버(내가 아닌 다른 백엔드 서버일 수도 있음)로 실시간 알림을
 * 전달하기 위해 SP-Framework의 WebSocket 프록시(ProxyData)를 사용합니다.
 */
public final class MailboxProxyService {

    /** 다른 서버들이 이 이벤트를 구분할 수 있도록 사용하는 이벤트 이름. */
    public static final String EVENT_MAIL_ADDED = "mail_added";

    private MailboxProxyService() {
    }

    /**
     * 모든 백엔드 서버에 "이 UUID의 플레이어에게 우편물이 도착했다"는 메시지를 방송합니다.
     * 실제로 그 플레이어가 접속해 있는 서버의 MailboxProxyListener만 반응해서 알림을 보여줍니다.
     */
    public static void notifyMailAdded(UUID targetUuid) {
        SPFramework framework = SPFramework.getInstance();
        if (framework == null || !framework.getConfigManager().isProxy()) {
            // 이 서버(또는 네트워크 전체)에서 프록시 기능이 꺼져 있으면 전송하지 않습니다.
            // (config.yaml의 proxy: true 로 활성화해야 동작합니다.)
            return;
        }
        ProxyData.sendToServer(SPMailbox.PROXY_CHANNEL, EVENT_MAIL_ADDED, targetUuid.toString());
    }
}
