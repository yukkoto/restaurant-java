package ru.mirea.restaurant.service;

import ru.mirea.restaurant.model.*;
import ru.mirea.restaurant.repository.CrudRepository;
import ru.mirea.restaurant.exception.BusinessException;
import java.util.List;

public class CustomerService {
    private final CrudRepository<Customer> customers;
    private final CrudRepository<RestaurantOrder> orders;

    public CustomerService(CrudRepository<Customer> customers, CrudRepository<RestaurantOrder> orders) {
        this.customers = customers;
        this.orders = orders;
    }
    public List<Customer> findAll() { return customers.findAll(); }
    public Customer findById(int id) { return customers.findById(id); }

    public Customer create(String name, String phone) {
        validate(0, name, phone);
        return customers.create(new Customer(0, name.trim(), phone.trim()));
    }
    public void update(int id, String name, String phone) {
        customers.findById(id);
        validate(id, name, phone);
        customers.update(new Customer(id, name.trim(), phone.trim()));
    }
    public void delete(int id) {
        customers.findById(id);
        if (orders.findAll().stream().anyMatch(o -> o.getCustomerId() == id))
            throw new BusinessException("Нельзя удалить клиента, у которого есть заказы.");
        customers.delete(id);
    }
    private void validate(int id, String name, String phone) {
        if (name == null || name.isBlank() || name.trim().length() > 100)
            throw new BusinessException("Имя должно содержать от 1 до 100 символов.");
        if (phone == null || !phone.trim().matches("\\+7[0-9]{10}"))
            throw new BusinessException("Телефон должен быть в формате +79001234567.");
        boolean duplicate = customers.findAll().stream()
                .anyMatch(c -> c.getId() != id && c.getPhone().equals(phone.trim()));
        if (duplicate) throw new BusinessException("Клиент с таким телефоном уже существует.");
    }
}
