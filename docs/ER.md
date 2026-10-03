# ER-диаграмма ресторана

Графический файл: [ER.svg](ER.svg). Его можно открыть в браузере и распечатать.

```mermaid
erDiagram
    customers ||--o{ restaurant_orders : "оформляет"
    customers {
        integer id PK "IDENTITY"
        varchar_100 name "NOT NULL, не пустое"
        varchar_12 phone UK "NOT NULL, +7 и 10 цифр"
    }
    restaurant_orders {
        integer id PK "IDENTITY"
        integer customer_id FK "NOT NULL, ON DELETE RESTRICT"
        integer table_number "NOT NULL, 1..20"
        varchar_500 description "NOT NULL, не пустое"
        numeric_10_2 total "NOT NULL, больше нуля"
        varchar_20 status "NOT NULL, 5 значений"
        timestamp created_at "NOT NULL, CURRENT_TIMESTAMP"
    }
```

Один клиент имеет **0..N заказов**. Каждый заказ принадлежит **ровно одному клиенту**. Связь: `restaurant_orders.customer_id → customers.id`. Все поля обеих таблиц обязательны; ID и время создания назначаются автоматически. Телефон уникален.
