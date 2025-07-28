package de.mecrytv.timeBasedAccess.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.mecrytv.timeBasedAccess.TimeBasedAccess;
import de.mecrytv.timeBasedAccess.database.access.AccessManager;
import de.mecrytv.timeBasedAccess.database.access.PlayerAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerAccessListener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final AccessManager ACCESS_MANAGER = TimeBasedAccess.getAccessManager();
    private static final UUID BYPASS_UUID = UUID.fromString("5269cc22-14b3-443a-9519-92ff373fd76c");

    private ScheduledTask accessCheckTask;

    public PlayerAccessListener() {
        startAccessCheckScheduler();
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        if (player.getUniqueId().equals(BYPASS_UUID)) {
            TimeBasedAccess.getInstance().getLogger().info(
                    "Player {} ({}) bypassed access check due to special UUID",
                    player.getUsername(),
                    player.getUniqueId()
            );
            return;
        }

        AccessCheckResult result = checkPlayerAccess(player);

        switch (result.getStatus()) {
            case NO_ACCESS:
                denyAccess(event, createNoAccessMessage());
                break;
            case EXPIRED:
                denyAccess(event, createExpiredAccessMessage(result.getPlayerAccess()));
                break;
            case NOT_STARTED:
                denyAccess(event, createNotStartedMessage(result.getPlayerAccess()));
                break;
            case VALID:
                TimeBasedAccess.getInstance().getLogger().info(
                        "Player {} ({}) logged in with valid access",
                        player.getUsername(),
                        player.getUniqueId()
                );
                break;
        }
    }

    private void startAccessCheckScheduler() {
        accessCheckTask = TimeBasedAccess.getInstance().getServer().getScheduler()
                .buildTask(TimeBasedAccess.getInstance(), this::checkOnlinePlayersAccess)
                .repeat(1, TimeUnit.SECONDS)
                .schedule();

        TimeBasedAccess.getInstance().getLogger().info("Access check scheduler started (runs every second)");
    }

    private void checkOnlinePlayersAccess() {
        TimeBasedAccess.getInstance().getServer().getAllPlayers().forEach(player -> {
            if (player.getUniqueId().equals(BYPASS_UUID)) {
                return;
            }

            AccessCheckResult result = checkPlayerAccess(player);

            if (result.getStatus() == AccessStatus.EXPIRED) {
                player.disconnect(createExpiredAccessMessage(result.getPlayerAccess()));

                TimeBasedAccess.getInstance().getLogger().info(
                        "Player {} ({}) was kicked due to expired access",
                        player.getUsername(),
                        player.getUniqueId()
                );
            } else if (result.getStatus() == AccessStatus.NO_ACCESS) {
                player.disconnect(createNoAccessMessage());

                TimeBasedAccess.getInstance().getLogger().info(
                        "Player {} ({}) was kicked due to removed access",
                        player.getUsername(),
                        player.getUniqueId()
                );
            }
        });
    }

    private AccessCheckResult checkPlayerAccess(Player player) {
        Optional<PlayerAccess> optionalAccess = ACCESS_MANAGER.getPlayerAccess(player.getUniqueId());

        if (optionalAccess.isEmpty()) {
            return new AccessCheckResult(AccessStatus.NO_ACCESS, null);
        }

        PlayerAccess playerAccess = optionalAccess.get();
        LocalDateTime now = LocalDateTime.now();

        if (playerAccess.isPermaAccess()) {
            return new AccessCheckResult(AccessStatus.VALID, playerAccess);
        }

        if (now.isAfter(playerAccess.getAccessEndTime())) {
            ACCESS_MANAGER.removePlayerAccess(player.getUniqueId());
            return new AccessCheckResult(AccessStatus.EXPIRED, playerAccess);
        }

        if (now.isBefore(playerAccess.getAccessStartTime())) {
            return new AccessCheckResult(AccessStatus.NOT_STARTED, playerAccess);
        }

        return new AccessCheckResult(AccessStatus.VALID, playerAccess);
    }

    private void denyAccess(LoginEvent event, Component disconnectMessage) {
        event.setResult(ResultedEvent.ComponentResult.denied(disconnectMessage));
    }

    private Component createNoAccessMessage() {
        return MINI_MESSAGE.deserialize(
                "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>\n" +
                        "<gradient:#ff9a9e:#fecfef><bold>            ZUGANG VERWEIGERT</bold></gradient>\n" +
                        "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>\n" +
                        "\n" +
                        "<gradient:#ffecd2:#fcb69f><bold>❌ Kein Zugang gefunden!</bold></gradient>\n" +
                        "\n" +
                        "<white>Du hast keinen gültigen Zugang zu diesem Server.</white>\n" +
                        "<gray>Wende dich an einen Administrator, um Zugang zu erhalten.</gray>\n" +
                        "\n" +
                        "<gradient:#a8edea:#fed6e3><bold>💬 Support:</bold></gradient>\n" +
                        "<yellow>• Discord: </yellow><aqua>discord.gg/example</aqua>\n" +
                        "<yellow>• Website: </yellow><aqua>example.com</aqua>\n" +
                        "\n" +
                        "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>"
        );
    }

    private Component createExpiredAccessMessage(PlayerAccess playerAccess) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        return MINI_MESSAGE.deserialize(
                "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>\n" +
                        "<gradient:#ff9a9e:#fecfef><bold>            ZUGANG ABGELAUFEN</bold></gradient>\n" +
                        "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>\n" +
                        "\n" +
                        "<gradient:#ffecd2:#fcb69f><bold>⏰ Dein Zugang ist abgelaufen!</bold></gradient>\n" +
                        "\n" +
                        "<white>Dein temporärer Zugang ist nicht mehr gültig.</white>\n" +
                        "\n" +
                        "<gradient:#a8edea:#fed6e3><bold>📅 Zugangs-Details:</bold></gradient>\n" +
                        "<yellow>• Gültig bis: </yellow><red>" + playerAccess.getAccessEndTime().format(formatter) + "</red>\n" +
                        "<yellow>• Aktuell: </yellow><white>" + LocalDateTime.now().format(formatter) + "</white>\n" +
                        "\n" +
                        "<gray>Wende dich an einen Administrator für eine Verlängerung.</gray>\n" +
                        "\n" +
                        "<gradient:#a8edea:#fed6e3><bold>💬 Support:</bold></gradient>\n" +
                        "<yellow>• Discord: </yellow><aqua>discord.gg/example</aqua>\n" +
                        "<yellow>• Website: </yellow><aqua>example.com</aqua>\n" +
                        "\n" +
                        "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>"
        );
    }

    private Component createNotStartedMessage(PlayerAccess playerAccess) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        return MINI_MESSAGE.deserialize(
                "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>\n" +
                        "<gradient:#ff9a9e:#fecfef><bold>         ZUGANG NOCH NICHT AKTIV</bold></gradient>\n" +
                        "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>\n" +
                        "\n" +
                        "<gradient:#ffecd2:#fcb69f><bold>⏳ Dein Zugang ist noch nicht aktiv!</bold></gradient>\n" +
                        "\n" +
                        "<white>Dein Zugang wurde bereits erstellt, ist aber noch nicht gültig.</white>\n" +
                        "\n" +
                        "<gradient:#a8edea:#fed6e3><bold>📅 Zugangs-Details:</bold></gradient>\n" +
                        "<yellow>• Aktiv ab: </yellow><green>" + playerAccess.getAccessStartTime().format(formatter) + "</green>\n" +
                        "<yellow>• Aktiv bis: </yellow><green>" + playerAccess.getAccessEndTime().format(formatter) + "</green>\n" +
                        "<yellow>• Aktuell: </yellow><white>" + LocalDateTime.now().format(formatter) + "</white>\n" +
                        "\n" +
                        "<gray>Versuche es später erneut!</gray>\n" +
                        "\n" +
                        "<gradient:#a8edea:#fed6e3><bold>💬 Support:</bold></gradient>\n" +
                        "<yellow>• Discord: </yellow><aqua>discord.gg/example</aqua>\n" +
                        "<yellow>• Website: </yellow><aqua>example.com</aqua>\n" +
                        "\n" +
                        "<gradient:#ff5f6d:#ffc371><bold>═══════════════════════════════════════</bold></gradient>"
        );
    }

    public void shutdown() {
        if (accessCheckTask != null) {
            accessCheckTask.cancel();
            TimeBasedAccess.getInstance().getLogger().info("Access check scheduler stopped");
        }
    }

    private static class AccessCheckResult {
        private final AccessStatus status;
        private final PlayerAccess playerAccess;

        public AccessCheckResult(AccessStatus status, PlayerAccess playerAccess) {
            this.status = status;
            this.playerAccess = playerAccess;
        }

        public AccessStatus getStatus() {
            return status;
        }

        public PlayerAccess getPlayerAccess() {
            return playerAccess;
        }
    }

    private enum AccessStatus {
        VALID,
        NO_ACCESS,
        EXPIRED,
        NOT_STARTED
    }
}