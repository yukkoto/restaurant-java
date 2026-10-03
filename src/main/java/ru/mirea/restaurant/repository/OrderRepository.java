package ru.mirea.restaurant.repository;

import ru.mirea.restaurant.model.*;
import ru.mirea.restaurant.exception.EntityNotFoundException;
import ru.mirea.restaurant.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository implements CrudRepository<RestaurantOrder> {
    private final DatabaseManager database;

    public OrderRepository(DatabaseManager database) { this.database = database; }

    @Override
    public RestaurantOrder create(RestaurantOrder item) {
        String sql = "INSERT INTO restaurant_orders (customer_id, table_number, description, total, status) VALUES (?, ?, ?, ?, ?) RETURNING *";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            fill(ps, item);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return map(rs);
            }
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public List<RestaurantOrder> findAll() {
        List<RestaurantOrder> result = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM restaurant_orders ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
            return result;
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public RestaurantOrder findById(int id) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM restaurant_orders WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                throw new EntityNotFoundException("Запись с ID " + id + " в restaurant_orders не найдена.");
            }
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public void update(RestaurantOrder item) {
        String sql = "UPDATE restaurant_orders SET customer_id = ?, table_number = ?, description = ?, total = ?, status = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            fill(ps, item);
            ps.setInt(6, item.getId());
            if (ps.executeUpdate() == 0) throw new EntityNotFoundException("Запись не найдена.");
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public void delete(int id) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM restaurant_orders WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new EntityNotFoundException("Запись не найдена.");
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    private void fill(PreparedStatement ps, RestaurantOrder item) throws SQLException {
        ps.setInt(1, item.getCustomerId());
        ps.setInt(2, item.getTableNumber());
        ps.setString(3, item.getDescription());
        ps.setBigDecimal(4, item.getTotal());
        ps.setString(5, item.getStatus().name());
    }

    private RestaurantOrder map(ResultSet rs) throws SQLException {
        return new RestaurantOrder(rs.getInt("id"), rs.getInt("customer_id"),
                rs.getInt("table_number"), rs.getString("description"), rs.getBigDecimal("total"),
                OrderStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toLocalDateTime());
    }
}
