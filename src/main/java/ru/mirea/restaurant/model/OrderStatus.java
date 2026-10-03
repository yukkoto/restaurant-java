package ru.mirea.restaurant.model;

public enum OrderStatus {
    NEW("Новый"), COOKING("Готовится"), SERVED("Подан"),
    PAID("Оплачен"), CANCELLED("Отменён");

    private final String title;

    OrderStatus(String title) { this.title = title; }
    public String getTitle() { return title; }
}
