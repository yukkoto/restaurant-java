package ru.mirea.restaurant.service;

import ru.mirea.restaurant.model.*;
import ru.mirea.restaurant.repository.CrudRepository;
import ru.mirea.restaurant.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class OrderService {
    private final CrudRepository<RestaurantOrder> orders;
    private final CrudRepository<Customer> customers;

    public OrderService(CrudRepository<RestaurantOrder> orders, CrudRepository<Customer> customers) {
        this.orders = orders;
        this.customers = customers;
    }
    public List<RestaurantOrder> findAll() { return orders.findAll(); }
    public RestaurantOrder findById(int id) { return orders.findById(id); }

    public RestaurantOrder create(int customerId, int table, String description, BigDecimal total) {
        validate(customerId, table, description, total);
        // Статус нового заказа всегда NEW, дату создания назначает база данных.
        return orders.create(new RestaurantOrder(0, customerId, table, description.trim(),
                total, OrderStatus.NEW, null));
    }
    public void update(int id, int customerId, int table, String description, BigDecimal total) {
        RestaurantOrder old = orders.findById(id);
        if (old.getStatus() != OrderStatus.NEW)
            throw new BusinessException("Изменять состав, клиента, стол и сумму можно только у нового заказа.");
        validate(customerId, table, description, total);
        orders.update(new RestaurantOrder(id, customerId, table, description.trim(),
                total, old.getStatus(), old.getCreatedAt()));
    }
    public void changeStatus(int id, OrderStatus next) {
        RestaurantOrder old = orders.findById(id);
        if (next == null) throw new BusinessException("Статус обязателен.");
        boolean allowed = switch (old.getStatus()) {
            case NEW -> next == OrderStatus.COOKING || next == OrderStatus.CANCELLED;
            case COOKING -> next == OrderStatus.SERVED || next == OrderStatus.CANCELLED;
            case SERVED -> next == OrderStatus.PAID;
            case PAID, CANCELLED -> false;
        };
        if (!allowed) throw new BusinessException("Запрещён переход: " + old.getStatus().getTitle()
                + " -> " + next.getTitle() + ". Порядок: Новый -> Готовится -> Подан -> Оплачен.");
        orders.update(new RestaurantOrder(id, old.getCustomerId(), old.getTableNumber(),
                old.getDescription(), old.getTotal(), next, old.getCreatedAt()));
    }
    public void delete(int id) {
        RestaurantOrder order = orders.findById(id);
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.CANCELLED)
            throw new BusinessException("Удалить можно только новый или отменённый заказ.");
        orders.delete(id);
    }
    private void validate(int customerId, int table, String description, BigDecimal total) {
        customers.findById(customerId);
        if (table < 1 || table > 20) throw new BusinessException("Номер стола должен быть от 1 до 20.");
        if (description == null || description.isBlank() || description.trim().length() > 500)
            throw new BusinessException("Состав заказа должен содержать от 1 до 500 символов.");
        if (total == null || total.signum() <= 0 || total.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new BusinessException("Сумма должна быть больше 0 и не больше 99999999.99 руб.");
        if (total.stripTrailingZeros().scale() > 2)
            throw new BusinessException("В сумме должно быть не больше двух знаков после запятой.");
    }
    private String searchText(String text) {
        if (text == null || text.isBlank()) throw new BusinessException("Введите непустой текст поиска.");
        return text.trim().toLowerCase(Locale.ROOT);
    }
    public List<RestaurantOrder> searchByDescription(String text) {
        String query = searchText(text);
        return orders.findAll().stream()
                .filter(o -> o.getDescription().toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    public List<RestaurantOrder> searchByCustomerName(String text) {
        String query = searchText(text);
        Set<Integer> ids = new HashSet<>();
        for (Customer customer : customers.findAll()) {
            if (customer.getName().toLowerCase(Locale.ROOT).contains(query)) ids.add(customer.getId());
        }
        return orders.findAll().stream().filter(o -> ids.contains(o.getCustomerId())).toList();
    }
    public List<RestaurantOrder> filterByStatus(OrderStatus status) {
        if (status == null) throw new BusinessException("Выберите статус.");
        return orders.findAll().stream().filter(o -> o.getStatus() == status).toList();
    }
    public List<RestaurantOrder> filterByTable(int table) {
        if (table < 1 || table > 20) throw new BusinessException("Номер стола должен быть от 1 до 20.");
        return orders.findAll().stream().filter(o -> o.getTableNumber() == table).toList();
    }
    public List<RestaurantOrder> filterByDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to))
            throw new BusinessException("Начальная дата не должна быть позже конечной.");
        return orders.findAll().stream().filter(o -> {
            LocalDate date = o.getCreatedAt().toLocalDate();
            return !date.isBefore(from) && !date.isAfter(to);
        }).toList();
    }
    public List<RestaurantOrder> sortByTotal() {
        return orders.findAll().stream().sorted(Comparator.comparing(RestaurantOrder::getTotal)
                .thenComparingInt(RestaurantOrder::getId)).toList();
    }
    public List<RestaurantOrder> sortByDate() {
        return orders.findAll().stream().sorted(Comparator.comparing(RestaurantOrder::getCreatedAt)
                .thenComparingInt(RestaurantOrder::getId)).toList();
    }
    public Map<String, String> statistics() {
        List<RestaurantOrder> all = orders.findAll();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("Всего клиентов", String.valueOf(customers.findAll().size()));
        result.put("Всего заказов", String.valueOf(all.size()));
        for (OrderStatus status : OrderStatus.values()) {
            long count = all.stream().filter(o -> o.getStatus() == status).count();
            result.put(status.getTitle(), String.valueOf(count));
        }
        BigDecimal revenue = BigDecimal.ZERO;
        for (RestaurantOrder order : all) {
            if (order.getStatus() == OrderStatus.PAID) revenue = revenue.add(order.getTotal());
        }
        result.put("Выручка по оплаченным заказам, руб.", revenue.setScale(2).toPlainString());
        return result;
    }
}
