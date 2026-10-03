package ru.mirea.restaurant.repository;

import ru.mirea.restaurant.model.*;
import ru.mirea.restaurant.exception.EntityNotFoundException;
import ru.mirea.restaurant.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository implements CrudRepository<Customer> {
    private final DatabaseManager database;

    public CustomerRepository(DatabaseManager database) { this.database = database; }

    @Override
    public Customer create(Customer item) {
        String sql = "INSERT INTO customers (name, phone) VALUES (?, ?) RETURNING *";
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
    public List<Customer> findAll() {
        List<Customer> result = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM customers ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
            return result;
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public Customer findById(int id) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM customers WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                throw new EntityNotFoundException("Запись с ID " + id + " в customers не найдена.");
            }
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public void update(Customer item) {
        String sql = "UPDATE customers SET name = ?, phone = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            fill(ps, item);
            ps.setInt(3, item.getId());
            if (ps.executeUpdate() == 0) throw new EntityNotFoundException("Запись не найдена.");
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    @Override
    public void delete(int id) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new EntityNotFoundException("Запись не найдена.");
        } catch (SQLException e) { throw DatabaseManager.error(e); }
    }

    private void fill(PreparedStatement ps, Customer item) throws SQLException {
        ps.setString(1, item.getName());
        ps.setString(2, item.getPhone());
    }

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("id"), rs.getString("name"), rs.getString("phone"));
    }
}
