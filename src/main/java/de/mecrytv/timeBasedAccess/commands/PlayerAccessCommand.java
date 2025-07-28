package de.mecrytv.timeBasedAccess.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.mecrytv.timeBasedAccess.TimeBasedAccess;
import de.mecrytv.timeBasedAccess.database.access.PlayerAccess;
import de.mecrytv.timeBasedAccess.utils.GeneralUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlayerAccessCommand implements SimpleCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String PERMISSION = "tbaccess.access";

    private static final String CREATE_TYPE = "create";
    private static final String REMOVE_TYPE = "remove";
    private static final List<String> COMMAND_TYPES = Arrays.asList(CREATE_TYPE, REMOVE_TYPE);

    private static final List<String> TIME_EXAMPLES = Arrays.asList("1d", "2h", "30m", "1h30m", "7d");
    private static final List<String> BOOLEAN_VALUES = Arrays.asList("true", "false");

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!isPlayer(source)) {
            source.sendMessage(Component.text("Only players can use this command!"));
            return;
        }

        Player player = (Player) source;

        if (!hasPermission(player)) {
            player.sendMessage(createErrorMessage("You don't have permission to use this command!"));
            return;
        }

        player.sendActionBar(Component.empty());

        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        String type = args[0].toLowerCase();

        switch (type) {
            case CREATE_TYPE:
                handleCreateCommand(player, args);
                break;
            case REMOVE_TYPE:
                handleRemoveCommand(player, args);
                break;
            default:
                sendInvalidTypeMessage(player);
                break;
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();

        return CompletableFuture.supplyAsync(() -> {
            if (args.length == 1) {
                return COMMAND_TYPES.stream()
                        .filter(type -> type.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 3 && CREATE_TYPE.equals(args[0].toLowerCase())) {
                return TIME_EXAMPLES.stream()
                        .filter(time -> time.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 4 && CREATE_TYPE.equals(args[0].toLowerCase())) {
                return BOOLEAN_VALUES.stream()
                        .filter(bool -> bool.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }

            return List.of();
        });
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length != 4) {
            sendCreateUsage(player);
            return;
        }

        String playerName = args[1];
        String time = args[2];
        boolean permaAccess;

        try {
            permaAccess = Boolean.parseBoolean(args[3]);
        } catch (Exception e) {
            player.sendMessage(createErrorMessage("Invalid boolean value! Use 'true' or 'false'."));
            return;
        }

        UUID playerUUID = GeneralUtils.getUUIDFromName(playerName);
        if (playerUUID == null) {
            player.sendMessage(createErrorMessage("Player not found!"));
            return;
        }

        LocalDateTime[] timeResult = GeneralUtils.parseTime(time);
        if (timeResult == null) {
            player.sendMessage(createErrorMessage("Invalid time format! Use format like: 1d, 2h, 30m, 45s"));
            return;
        }

        LocalDateTime startTime = timeResult[0];
        LocalDateTime endTime = timeResult[1];

        if (TimeBasedAccess.getAccessManager().getPlayerAccess(playerUUID).isEmpty()) {
            PlayerAccess playerAccess = new PlayerAccess(playerUUID, playerName, startTime, endTime, permaAccess);
            TimeBasedAccess.getAccessManager().createPlayerAccess(playerAccess);

            player.sendMessage(createSuccessMessage("Player access created successfully for " + playerName + "!"));
        } else {
            player.sendMessage(createErrorMessage("Player already has access!"));
        }
    }

    private void handleRemoveCommand(Player player, String[] args) {
        if (args.length != 2) {
            sendRemoveUsage(player);
            return;
        }

        String playerName = args[1];
        UUID playerUUID = GeneralUtils.getUUIDFromName(playerName);

        if (playerUUID == null) {
            player.sendMessage(createErrorMessage("Player not found!"));
            return;
        }

        if (TimeBasedAccess.getAccessManager().getPlayerAccess(playerUUID).isPresent()) {
            TimeBasedAccess.getAccessManager().removePlayerAccess(playerUUID);
            player.sendMessage(createSuccessMessage("Player access removed successfully for " + playerName + "!"));
        } else {
            player.sendMessage(createErrorMessage("Player does not have access!"));
        }
    }

    private boolean isPlayer(CommandSource source) {
        return source instanceof Player;
    }

    private boolean hasPermission(Player player) {
        return player.hasPermission(PERMISSION);
    }

    private Component createErrorMessage(String message) {
        return TimeBasedAccess.getPrefix().append(MINI_MESSAGE.deserialize("<red>" + message + "</red>"));
    }

    private Component createSuccessMessage(String message) {
        return TimeBasedAccess.getPrefix().append(MINI_MESSAGE.deserialize("<green>" + message + "</green>"));
    }

    private void sendUsage(Player player) {
        player.sendMessage(TimeBasedAccess.getPrefix().append(MINI_MESSAGE.deserialize(
                "<gradient:#ff5f6d:#ffc371><shadow:#000000><bold>Usage:</bold></shadow></gradient>\n" +
                        "<gradient:#89f7fe:#66a6ff><italic>/playerAccess create</italic></gradient> <gradient:#fffacd:#ffdd57><playerName></gradient> <gradient:#c2ffd8:#61a6ab><time></gradient> <gradient:#fbc2eb:#a6c1ee><permaAccess></gradient>\n" +
                        "<gradient:#89f7fe:#66a6ff><italic>/playerAccess remove</italic></gradient> <gradient:#fffacd:#ffdd57><playerName></gradient>"
        )));
    }

    private void sendCreateUsage(Player player) {
        player.sendMessage(TimeBasedAccess.getPrefix().append(MINI_MESSAGE.deserialize(
                "<gradient:#ff5f6d:#ffc371><shadow:#000000><bold>Usage:</bold></shadow></gradient> " +
                        "<gradient:#89f7fe:#66a6ff><italic>/playerAccess create</italic></gradient> " +
                        "<gradient:#fffacd:#ffdd57><playerName></gradient> " +
                        "<gradient:#c2ffd8:#61a6ab><time></gradient> " +
                        "<gradient:#fbc2eb:#a6c1ee><permaAccess></gradient>"
        )));
    }

    private void sendRemoveUsage(Player player) {
        player.sendMessage(TimeBasedAccess.getPrefix().append(MINI_MESSAGE.deserialize(
                "<gradient:#ff5f6d:#ffc371><shadow:#000000><bold>Usage:</bold></shadow></gradient> " +
                        "<gradient:#89f7fe:#66a6ff><italic>/playerAccess remove</italic></gradient> " +
                        "<gradient:#fffacd:#ffdd57><playerName></gradient>"
        )));
    }

    private void sendInvalidTypeMessage(Player player) {
        player.sendMessage(createErrorMessage("Invalid command type! Use 'create' or 'remove'."));
    }
}