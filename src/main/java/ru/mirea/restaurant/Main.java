package ru.mirea.restaurant;

import ru.mirea.restaurant.exception.DataAccessException;
import ru.mirea.restaurant.model.*;
import ru.mirea.restaurant.repository.*;
import ru.mirea.restaurant.service.*;
import ru.mirea.restaurant.ui.ConsoleUi;
import ru.mirea.restaurant.util.DatabaseManager;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path config = Path.of(args.length == 0 ? "db.properties" : args[0]);
        try {
            DatabaseManager database = new DatabaseManager(config);
            CrudRepository<Customer> customers = new CustomerRepository(database);
            CrudRepository<RestaurantOrder> orders = new OrderRepository(database);
            CustomerService customerService = new CustomerService(customers, orders);
            OrderService orderService = new OrderService(orders, customers);
            ReportService reports = new ReportService(customerService, orderService, new DatabaseInfoRepository(database));
            new ConsoleUi(customerService, orderService, reports).run();
        } catch (DataAccessException e) {
            System.out.println("Ошибка настройки: " + e.getMessage());
            System.out.println("Исправьте файл настроек и запустите программу снова.");
        }
    }
}
