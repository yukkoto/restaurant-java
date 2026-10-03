package ru.mirea.restaurant.repository;

import ru.mirea.restaurant.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseInfoRepository {
    private final DatabaseManager database;
    public DatabaseInfoRepository(DatabaseManager database) { this.database = database; }

    public List<String> findTableNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = ? ORDER BY table_name";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "public");
            ps.setString(2, "BASE TABLE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("table_name"));
            }
            return names;
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }
}
