package ru.mirea.restaurant.ui;

import ru.mirea.restaurant.exception.*;
import ru.mirea.restaurant.model.OrderStatus;
import ru.mirea.restaurant.service.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleUi {
    private final CustomerService customers;
    private final OrderService orders;
    private final ReportService reports;
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public ConsoleUi(CustomerService customers, OrderService orders, ReportService reports) {
        this.customers = customers;
        this.orders = orders;
        this.reports = reports;
    }
    public void run() {
        try {
            boolean running = true;
            while (running) {
                System.out.println("""
                        
========== РЕСТОРАН ==========
                        1. Клиенты
                        2. Заказы
                        3. Поиск заказов
                        4. Фильтрация заказов
                        5. Сортировка заказов
                        6. Статистика
                        7. Экспорт в Excel
                        8. Таблицы базы данных
                        0. Выход""");
                try {
                    switch (number("Выберите действие: ")) {
                        case 1 -> customerMenu();
                        case 2 -> orderMenu();
                        case 3 -> searchMenu();
                        case 4 -> filterMenu();
                        case 5 -> sortMenu();
                        case 6 -> orders.statistics().forEach((key, value) -> System.out.println(key + ": " + value));
                        case 7 -> System.out.println("Файл создан: " + reports.export());
                        case 8 -> {
                            System.out.println("Таблицы: " + reports.tableNames());
                            System.out.println("--- Клиенты ---"); print(customers.findAll());
                            System.out.println("--- Заказы ---"); print(orders.findAll());
                        }
                        case 0 -> running = false;
                        default -> System.out.println("Нет такого пункта меню.");
                    }
                } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Не удалось сохранить Excel. Проверьте доступ к папке exports: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Ввод завершён.");
        }
        System.out.println("До свидания!");
    }
    private void customerMenu() {
        while (true) {
            System.out.println("\nКЛИЕНТЫ: 1 — создать; 2 — все; 3 — по ID; 4 — изменить; 5 — удалить; 0 — назад");
            int choice = number("Действие: ");
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> System.out.println("Создан: " + customers.create(text("Имя: "), text("Телефон (+79001234567): ")));
                    case 2 -> print(customers.findAll());
                    case 3 -> System.out.println(customers.findById(number("ID клиента: ")));
                    case 4 -> {
                        int id = number("ID клиента: ");
                        System.out.println(customers.findById(id));
                        customers.update(id, text("Новое имя: "), text("Новый телефон: "));
                        System.out.println("Клиент изменён.");
                    }
                    case 5 -> {
                        int id = number("ID клиента: ");
                        System.out.println(customers.findById(id));
                        if (confirm()) { customers.delete(id); System.out.println("Клиент удалён."); }
                    }
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
    private void orderMenu() {
        while (true) {
            System.out.println("\nЗАКАЗЫ: 1 — создать; 2 — все; 3 — по ID; 4 — изменить данные; 5 — удалить; 6 — изменить статус; 0 — назад");
            int choice = number("Действие: ");
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> {
                        print(customers.findAll());
                        System.out.println("Создан: " + orders.create(number("ID клиента: "), number("Номер стола (1–20): "),
                                text("Состав заказа: "), money()));
                    }
                    case 2 -> print(orders.findAll());
                    case 3 -> System.out.println(orders.findById(number("ID заказа: ")));
                    case 4 -> {
                        int id = number("ID заказа: ");
                        System.out.println(orders.findById(id));
                        orders.update(id, number("ID клиента: "), number("Номер стола: "), text("Состав заказа: "), money());
                        System.out.println("Заказ изменён.");
                    }
                    case 5 -> {
                        int id = number("ID заказа: ");
                        System.out.println(orders.findById(id));
                        if (confirm()) { orders.delete(id); System.out.println("Заказ удалён."); }
                    }
                    case 6 -> {
                        int id = number("ID заказа: ");
                        System.out.println(orders.findById(id));
                        orders.changeStatus(id, status());
                        System.out.println("Статус изменён.");
                    }
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
    private void searchMenu() {
        while (true) {
            System.out.println("\nПОИСК: 1 — по составу заказа; 2 — по имени клиента; 0 — назад");
            int choice = number("Действие: ");
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> print(orders.searchByDescription(text("Часть состава: ")));
                    case 2 -> print(orders.searchByCustomerName(text("Часть имени: ")));
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | DataAccessException e) { System.out.println("Ошибка: " + e.getMessage()); }
        }
    }
    private void filterMenu() {
        while (true) {
            System.out.println("\nФИЛЬТРЫ: 1 — статус; 2 — стол; 3 — диапазон дат включительно; 0 — назад");
            int choice = number("Действие: ");
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> print(orders.filterByStatus(status()));
                    case 2 -> print(orders.filterByTable(number("Номер стола: ")));
                    case 3 -> print(orders.filterByDates(date("От (ГГГГ-ММ-ДД): "), date("До (ГГГГ-ММ-ДД): ")));
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | DataAccessException e) { System.out.println("Ошибка: " + e.getMessage()); }
        }
    }
    private void sortMenu() {
        while (true) {
            System.out.println("\nСОРТИРОВКА: 1 — сумма по возрастанию; 2 — дата от старых к новым; 0 — назад");
            int choice = number("Действие: ");
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> print(orders.sortByTotal());
                    case 2 -> print(orders.sortByDate());
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (DataAccessException e) { System.out.println("Ошибка: " + e.getMessage()); }
        }
    }
    private String text(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    private int number(String prompt) {
        while (true) {
            try { return Integer.parseInt(text(prompt)); }
            catch (NumberFormatException e) { System.out.println("Ошибка: нужно целое число в диапазоне int."); }
        }
    }
    private BigDecimal money() {
        while (true) {
            String value = text("Сумма в рублях (например, 850.50): ").replace(',', '.');
            if (value.matches("-?[0-9]{1,8}(\\.[0-9]{1,2})?")) return new BigDecimal(value);
            System.out.println("Ошибка: введите число, максимум 8 цифр до и 2 после запятой.");
        }
    }
    private LocalDate date(String prompt) {
        while (true) {
            try { return LocalDate.parse(text(prompt)); }
            catch (DateTimeParseException e) { System.out.println("Ошибка: введите существующую дату, например 2026-09-20."); }
        }
    }
    private OrderStatus status() {
        OrderStatus[] statuses = OrderStatus.values();
        for (int i = 0; i < statuses.length; i++) System.out.println((i + 1) + ". " + statuses[i].getTitle());
        while (true) {
            int index = number("Номер статуса: ");
            if (index >= 1 && index <= statuses.length) return statuses[index - 1];
            System.out.println("Ошибка: статус должен быть от 1 до " + statuses.length + ".");
        }
    }
    private boolean confirm() { return text("Удалить? Введите да: ").equalsIgnoreCase("да"); }
    private void print(List<?> items) {
        if (items.isEmpty()) System.out.println("Записи не найдены.");
        else items.forEach(System.out::println);
    }
}
