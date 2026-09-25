package net.teujaem.mailbox.listener;

import net.teujaem.mailbox.SPMailbox;
import net.teujaem.mailbox.proxy.MailboxProxyService;
import net.teujaem.spFramework.api.event.ProxyEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * 다른 백엔드 서버가 ProxyData.sendToServer(...)로 방송한 "우편물 도착" 메시지를
 * 받는 리스너입니다. WebSocket을 통해 전체 서버에 브로드캐스트되지만,
 * 실제로 그 플레이어가 접속해 있는 서버에서만 알림이 표시됩니다.
 */
public class MailboxProxyListener implements Listener {

    public MailboxProxyListener(SPMailbox plugin) {
        // 현재는 별도 상태가 필요하지 않지만, 확장을 위해 플러그인 인스턴스를 받아둡니다.
    }

    @EventHandler
    public void onProxyEvent(ProxyEvent event) {
        if (!SPMailbox.PROXY_CHANNEL.equals(event.getPluginName())) {
            return;
        }
        if (!MailboxProxyService.EVENT_MAIL_ADDED.equals(event.getEventName())) {
            return;
        }

        Object value = event.getValue();
        if (!(value instanceof String uuidString)) {
            return;
        }

        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a[!]&f 새 우편물이 도착했습니다! &b/우편함&f 으로 확인해보세요."));
        }
    }
}
