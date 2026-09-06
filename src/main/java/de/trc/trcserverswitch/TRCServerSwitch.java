package de.trc.trcserverswitch;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Plugin(
        id = "trc-serverswitch",
        name = "TRC",
        version = "1.0.0",
        description = "Switches between servers using commands",
        authors = {"trc"}
)
public final class TRCServerSwitch {

    private static final String LOBBY_SERVER_NAME = "lobby";
    private static final String POTATO_SERVER_NAME = "potato_craft";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private ConfigManager configManager;

    @Inject
    public TRCServerSwitch(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Config laden
        try {
            this.configManager = new ConfigManager(dataDirectory);
        } catch (IOException e) {
            logger.error("Konnte die Config.yml nicht laden!", e);
            return;
        }

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("lobby")
                        .plugin(this)
                        .build(),
                new ServerSwitchCommand(LOBBY_SERVER_NAME)
        );

        ServerSwitchCommand potatoCommand = new ServerSwitchCommand(POTATO_SERVER_NAME);

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("potato")
                        .plugin(this)
                        .build(),
                potatoCommand
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("reconnect")
                        .plugin(this)
                        .build(),
                potatoCommand
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("potato-craft")
                        .plugin(this)
                        .build(),
                potatoCommand
        );

        logger.info(
                "TRC ServerSwitch Velocity enabled. Targets: '{}' (/lobby), '{}' (/potato, /reconnect, /potato-craft).",
                LOBBY_SERVER_NAME, POTATO_SERVER_NAME
        );
    }

    private final class ServerSwitchCommand implements SimpleCommand {

        private final String targetServerName;

        private ServerSwitchCommand(String targetServerName) {
            this.targetServerName = targetServerName;
        }

        @Override
        public void execute(Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                String msg = configManager.message("only-players");
                invocation.source().sendMessage(miniMessage.deserialize(msg));
                return;
            }

            Optional<RegisteredServer> targetServer = server.getServer(targetServerName);

            if (targetServer.isEmpty()) {
                String msg = configManager.message("server-unavailable", "{server}", targetServerName);
                player.sendMessage(miniMessage.deserialize(msg));

                logger.warn(
                        "Player {} tried to connect to '{}', but the server is not registered in velocity.toml.",
                        player.getUsername(), targetServerName
                );
                return;
            }

            RegisteredServer target = targetServer.get();

            if (player.getCurrentServer().isPresent()
                    && player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(targetServerName)) {
                String msg = configManager.message("already-connected", "{server}", targetServerName);
                player.sendMessage(miniMessage.deserialize(msg));
                return;
            }

            player.createConnectionRequest(target).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    String msg = configManager.message("connect-success", "{server}", targetServerName);
                    player.sendMessage(miniMessage.deserialize(msg));
                } else {
                    String reason = result.getReasonComponent()
                            .map(miniMessage::serialize)
                            .orElse("Unknown reason.");

                    String msg = configManager.message("connect-failed", "{server}", targetServerName, "{reason}", reason);
                    player.sendMessage(miniMessage.deserialize(msg));

                    logger.warn("Connection of {} to '{}' failed: {}", player.getUsername(), targetServerName, reason);
                }
            });
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return true;
        }
    }
}
