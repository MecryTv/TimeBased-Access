package de.mecrytv.timeBasedAccess.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.mecrytv.timeBasedAccess.TimeBasedAccess;
import org.spongepowered.configurate.ConfigurationNode;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private HikariDataSource dataSource;

    public DatabaseManager() {
        ConfigurationNode cfg = TimeBasedAccess.getInstance().getConfig().node("mysql");

        String host = cfg.node("host").getString("localhost");
        int port = cfg.node("port").getInt(3306);
        String username = cfg.node("user").getString("admin");
        String password = cfg.node("password").getString("mysql05");
        String database = cfg.node("database").getString("tbaccess");

        HikariConfig mysqlConf = new HikariConfig();

        mysqlConf.setUsername(username);
        mysqlConf.setPassword(password);

        mysqlConf.setConnectionTimeout(2000);
        mysqlConf.setMaximumPoolSize(10);
        mysqlConf.setDriverClassName("com.mysql.cj.jdbc.Driver");

        String jdbcURL = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&serverTimezone=Europe/Berlin&useSSL=false";
        mysqlConf.setJdbcUrl(jdbcURL);

        dataSource = new HikariDataSource(mysqlConf);

        try {
            Connection connection = getConnection();
            closeConnection(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("MySQL-Initialisierung fehlgeschlagen", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            TimeBasedAccess.getInstance().getLogger().warn("Error closing connection: " + e.getMessage());
        }
    }

    public void shutDown() {
        dataSource.close();
    }
}
