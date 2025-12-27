package com.edutech.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static HikariDataSource dataSource;

    public static void init() {
        // get data from envrimonet variable for production
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASSWORD");
        
        // Default fallback for local development if env vars are missing
        if (url == null) url = "jdbc:mysql://localhost:3306/exam_system_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        if (user == null) user = "root";
        if (pass == null) pass = "exam_system_passwd";
        
        HikariConfig config = new HikariConfig();
        // MySQL Configuration
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);        
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Optimization properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        
        createTables();
        seedAdmin();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) dataSource.close();
    }

    private static void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // MySQL Syntax
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(50) UNIQUE, " +
                    "password VARCHAR(255), " +
                    "role VARCHAR(20))");

            stmt.execute("CREATE TABLE IF NOT EXISTS exams (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "title VARCHAR(255), " +
                    "description TEXT, " +
                    "duration_minutes INT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS questions (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "exam_id VARCHAR(36), " +
                    "text TEXT, " +
                    "correct_option_id VARCHAR(36), " +
                    "FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS options (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "question_id VARCHAR(36), " +
                    "text VARCHAR(255), " +
                    "FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS results (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "student_id VARCHAR(36), " +
                    "exam_id VARCHAR(36), " +
                    "score INT, " +
                    "total_questions INT, " +
                    "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            System.out.println(">> MySQL Tables Verified/Created.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void seedAdmin() {
        try (Connection conn = getConnection()) {
            String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
            // MySQL "Insert Ignore" or "On Duplicate Key" logic
            String sql = "INSERT INTO users (id, username, password, role) " +
                         "VALUES ('admin-uuid', 'admin', ?, 'ADMIN') " +
                         "ON DUPLICATE KEY UPDATE username=username";
            var ps = conn.prepareStatement(sql);
            ps.setString(1, hash);
            ps.executeUpdate();
            System.out.println(">> Admin account checked.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
