package net.teujaem.mailbox;

import net.teujaem.mailbox.command.MailboxCommand;
import net.teujaem.mailbox.entity.MailboxItem;
import net.teujaem.mailbox.listener.MailboxGuiListener;
import net.teujaem.mailbox.listener.MailboxProxyListener;
import net.teujaem.mailbox.repository.MailboxRepository;
import net.teujaem.jpalib.jpa.JpaManager;
import net.teujaem.spFramework.SPFramework;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SP-Framework(DB + 프록시)를 사용하는 우편함 플러그인의 메인 클래스.
 * plugin.yml의 depend: [SP-Framework] 로 인해, 이 클래스의 onEnable이 호출되는
 * 시점에는 SP-Framework가 이미 활성화되어 DB/프록시 설정이 로드되어 있습니다.
 */
public final class SPMailbox extends JavaPlugin {

    /** 프록시 메시지에서 이 플러그인을 식별하기 위한 채널(=pluginName) 이름. */
    public static final String PROXY_CHANNEL = "SP-Mailbox";

    private static SPMailbox instance;
    private MailboxRepository repository;

    @Override
    public void onEnable() {
        instance = this;

        SPFramework framework = SPFramework.getInstance();
        if (framework == null) {
            getLogger().severe("SP-Framework를 찾을 수 없습니다. 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // SP-Framework의 DatabaseController를 통해 MailboxItem 엔티티용 JpaManager를 생성합니다.
        // (내부적으로 config.yaml의 database 설정으로 MariaDB에 연결하고 테이블을 자동 생성/갱신합니다.)
        JpaManager jpaManager = framework.getDatabaseController().createJpaManager(MailboxItem.class);
        this.repository = new MailboxRepository(jpaManager);

        getServer().getPluginManager().registerEvents(new MailboxGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new MailboxProxyListener(this), this);

        MailboxCommand commandExecutor = new MailboxCommand(this);
        var pluginCommand = getCommand("우편함");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(commandExecutor);
            pluginCommand.setTabCompleter(commandExecutor);
        }

        getLogger().info("SP-Mailbox가 SP-Framework의 DB/프록시와 함께 활성화되었습니다. (proxy="
                + framework.getConfigManager().isProxy() + ")");
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static SPMailbox getInstance() {
        return instance;
    }

    public MailboxRepository getRepository() {
        return repository;
    }
}
