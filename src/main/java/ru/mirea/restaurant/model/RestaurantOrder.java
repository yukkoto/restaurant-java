package ru.mirea.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RestaurantOrder {
    private final int id;
    private final int customerId;
    private final int tableNumber;
    private final String description;
    private final BigDecimal total;
    private final OrderStatus status;
    private final LocalDateTime createdAt;

    public RestaurantOrder(int id, int customerId, int tableNumber, String description,
                           BigDecimal total, OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.tableNumber = tableNumber;
        this.description = description;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }
    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public int getTableNumber() { return tableNumber; }
    public String getDescription() { return description; }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "ID: " + id + " | клиент: " + customerId + " | стол: " + tableNumber
                + " | " + description + " | " + total + " руб. | " + status.getTitle()
                + " | " + createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
