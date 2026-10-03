package ru.mirea.restaurant.repository;

import java.util.List;

// Общий договор: что должен уметь любой репозиторий наших сущностей.
public interface CrudRepository<T> {
    T create(T item);
    List<T> findAll();
    T findById(int id);
    void update(T item);
    void delete(int id);
}
