package de.mecrytv.timeBasedAccess.database.access;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerAccess {

    private UUID playerUUID;
    private String playerName;
    private LocalDateTime accessStartTime;
    private LocalDateTime accessEndTime;
    private boolean permaAccess;

    public PlayerAccess(UUID playerUUID, String playerName, LocalDateTime accessStartTime, LocalDateTime accessEndTime, boolean permaAccess) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.accessStartTime = accessStartTime;
        this.accessEndTime = accessEndTime;
        this.permaAccess = permaAccess;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public LocalDateTime getAccessStartTime() {
        return accessStartTime;
    }

    public void setAccessStartTime(LocalDateTime accessStartTime) {
        this.accessStartTime = accessStartTime;
    }

    public LocalDateTime getAccessEndTime() {
        return accessEndTime;
    }

    public void setAccessEndTime(LocalDateTime accessEndTime) {
        this.accessEndTime = accessEndTime;
    }

    public boolean isPermaAccess() {
        return permaAccess;
    }

    public void setPermaAccess(boolean permaAccess) {
        this.permaAccess = permaAccess;
    }
}
