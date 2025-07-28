package de.mecrytv.timeBasedAccess;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.mecrytv.timeBasedAccess.commands.PlayerAccessCommand;
import de.mecrytv.timeBasedAccess.database.DatabaseManager;
import de.mecrytv.timeBasedAccess.database.access.AccessManager;
import de.mecrytv.timeBasedAccess.database.access.PlayerAccess;
import de.mecrytv.timeBasedAccess.listeners.PlayerAccessListener;
import de.mecrytv.timeBasedAccess.utils.LogWithColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Plugin(
        id = "timebasedaccess",
        name = "TimeBasedAccess",
        version = "1.0,0",
        description = "Velocity Plugin for gave Players access to the server based on time",
        authors = {"MecryTv"})
public class TimeBasedAccess {

    private final Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;

    private static TimeBasedAccess instance;
    private ConfigurationNode config;
    private static MiniMessage miniMessage = MiniMessage.miniMessage();
    private static Component prefix;

    private static DatabaseManager databaseManager;
    private static AccessManager accessManager;
    private PlayerAccessListener playerAccessListener;

    @Inject
    public TimeBasedAccess(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        instance = this;
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        startLog();
        loadConfig();

        databaseManager = new DatabaseManager();
        accessManager = new AccessManager();

        server.getCommandManager().register("playerAccess", new PlayerAccessCommand());
        server.getEventManager().register(this, new PlayerAccessListener());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (playerAccessListener != null) {
            playerAccessListener.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutDown();
        }

        logger.info(LogWithColor.color("Plugin has been disabled!", LogWithColor.RED));
    }

    private void startLog() {
        String[] tbAccess = {
                "████████╗██████╗       █████╗  ██████╗ ██████╗███████╗███████╗███████╗",
                "╚══██╔══╝██╔══██╗     ██╔══██╗██╔════╝██╔════╝██╔════╝██╔════╝██╔════╝",
                "   ██║   ██████╔╝     ███████║██║     ██║     █████╗  ███████╗███████╗",
                "   ██║   ██╔══██╗     ██╔══██║██║     ██║     ██╔══╝  ╚════██║╚════██║",
                "   ██║   ██████╔╝     ██║  ██║╚██████╗╚██████╗███████╗███████║███████║",
                "   ╚═╝   ╚═════╝      ╚═╝  ╚═╝ ╚═════╝ ╚═════╝╚══════╝╚══════╝╚══════╝",
                "                                                                     ",
                "                              TB-Access                             ",
                "                          Running on Velocity                       "
        };
        for (String line: tbAccess) {
            logger.info(LogWithColor.color(line, LogWithColor.GREEN));
        }
        logger.info(LogWithColor.color("Developed by MecryTv", LogWithColor.GOLD));
        logger.info(LogWithColor.color("Plugin has been enabled!", LogWithColor.GREEN));
    }

    private void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configFile = dataDirectory.resolve("config.yml");

            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        logger.warn("config.yml konnte nicht im Ressourcenordner gefunden werden.");
                    }
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .build();

            config = loader.load();
            logger.info("Konfiguration erfolgreich geladen.");

            String prefixString = config.node("prefix").getString();
            prefix = miniMessage.deserialize(prefixString);

        } catch (IOException e) {
            logger.error("Fehler beim Laden der Konfiguration: ", e);
        }

    }

    public static TimeBasedAccess getInstance() {
        return instance;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigurationNode getConfig() {
        return config;
    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static AccessManager getAccessManager() {
        return accessManager;
    }

    public static Component getPrefix() {
        return prefix;
    }
}
