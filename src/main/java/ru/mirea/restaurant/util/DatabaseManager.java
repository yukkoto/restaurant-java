package ru.mirea.restaurant.util;

import ru.mirea.restaurant.exception.DataAccessException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private final Properties properties = new Properties();

    public DatabaseManager(Path config) {
        try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new DataAccessException("Не удалось прочитать " + config + ". Проверьте путь к настройкам.", e);
        }
        for (String key : new String[]{"url", "user", "password"}) {
            if (properties.getProperty(key) == null) {
                throw new DataAccessException("В настройках отсутствует поле " + key, null);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(properties.getProperty("url"),
                properties.getProperty("user"), properties.getProperty("password"));
    }

    // SQLState — код вида ошибки, который возвращает PostgreSQL.
    public static DataAccessException error(SQLException e) {
        String state = e.getSQLState();
        String message;
        if ("23505".equals(state)) message = "Такой телефон уже существует (нарушение уникальности).";
        else if ("23503".equals(state)) message = "Нарушена связь: клиент не существует или у него есть заказы.";
        else if ("23514".equals(state) || "23502".equals(state)) message = "Данные нарушают ограничения таблицы.";
        else if (state != null && (state.startsWith("08") || state.startsWith("28") || state.equals("3D000")))
            message = "Не удалось подключиться к БД. Проверьте запуск PostgreSQL, имя базы, логин и пароль.";
        else message = "Ошибка SQL. Проверьте, выполнен ли скрипт sql/01_schema.sql. Код: " + state;
        return new DataAccessException(message, e);
    }
}
