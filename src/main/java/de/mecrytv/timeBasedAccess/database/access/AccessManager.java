package de.mecrytv.timeBasedAccess.database.access;

import de.mecrytv.timeBasedAccess.TimeBasedAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AccessManager {

    public AccessManager() {
        try {
            Connection connection = TimeBasedAccess.getDatabaseManager().getConnection();

            String sql = "CREATE TABLE IF NOT EXISTS `tbaccess` (`ID` INT AUTO_INCREMENT, `playerUUID` VARCHAR(36) NOT NULL UNIQUE, `playerName` VARCHAR(16) NOT NULL, `accessStartTime` DATETIME NOT NULL, `accessEndTime` DATETIME NOT NULL, `permaAccess` BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY (`ID`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();

            TimeBasedAccess.getDatabaseManager().closeConnection(connection);
        } catch (Exception e) {
            TimeBasedAccess.getInstance().getLogger().error("Error initializing AccessManager: ", e);
        }
    }

    public PlayerAccess createPlayerAccess(PlayerAccess playerAccess) {
        try (Connection connection = TimeBasedAccess.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tbaccess (playerUUID, playerName, accessStartTime, accessEndTime, permaAccess) VALUES (?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE playerName = VALUES(playerName), accessStartTime = VALUES(accessStartTime), " +
                             "accessEndTime = VALUES(accessEndTime), permaAccess = VALUES(permaAccess)")) {

            statement.setString(1, playerAccess.getPlayerUUID().toString());
            statement.setString(2, playerAccess.getPlayerName());
            statement.setObject(3, playerAccess.getAccessStartTime());
            statement.setObject(4, playerAccess.getAccessEndTime());
            statement.setBoolean(5, playerAccess.isPermaAccess());

            statement.executeUpdate();

        } catch (Exception e) {
            TimeBasedAccess.getInstance().getLogger().error("Error creating PlayerAccess: ", e);
        }
        return playerAccess;
    }

    public Optional<PlayerAccess> getPlayerAccess(UUID playerUUID) {
        try (Connection connection = TimeBasedAccess.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM tbaccess WHERE playerUUID = ?")) {

            statement.setString(1, playerUUID.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    PlayerAccess access = new PlayerAccess(
                            UUID.fromString(resultSet.getString("playerUUID")),
                            resultSet.getString("playerName"),
                            resultSet.getObject("accessStartTime", LocalDateTime.class),
                            resultSet.getObject("accessEndTime", LocalDateTime.class),
                            resultSet.getBoolean("permaAccess")
                    );
                    return Optional.of(access);
                }
            }

        } catch (Exception e) {
            TimeBasedAccess.getInstance().getLogger().error("Error retrieving PlayerAccess: ", e);
        }

        return Optional.empty();
    }

    public boolean removePlayerAccess(UUID playerUUID) {
        try (Connection connection = TimeBasedAccess.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM tbaccess WHERE playerUUID = ?")) {

            statement.setString(1, playerUUID.toString());
            int rowsAffected = statement.executeUpdate();

            return rowsAffected > 0;

        } catch (Exception e) {
            TimeBasedAccess.getInstance().getLogger().error("Error removing PlayerAccess: ", e);
            return false;
        }
    }

    public boolean hasValidAccess(UUID playerUUID) {
        Optional<PlayerAccess> access = getPlayerAccess(playerUUID);

        if (access.isEmpty()) {
            return false;
        }

        PlayerAccess playerAccess = access.get();

        if (playerAccess.isPermaAccess()) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(playerAccess.getAccessStartTime()) && now.isBefore(playerAccess.getAccessEndTime());
    }

    public int cleanupExpiredAccesses() {
        try (Connection connection = TimeBasedAccess.getDatabaseManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM tbaccess WHERE accessEndTime < ? AND permaAccess = FALSE")) {

            statement.setObject(1, LocalDateTime.now());
            return statement.executeUpdate();

        } catch (Exception e) {
            TimeBasedAccess.getInstance().getLogger().error("Error cleaning up expired accesses: ", e);
            return 0;
        }
    }
}