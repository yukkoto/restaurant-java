-- Учебные вымышленные данные. Запускать ОДИН РАЗ после создания пустых таблиц.
-- Транзакция не позволит оставить половину набора при ошибке повторного запуска.
BEGIN;
INSERT INTO customers (name, phone) VALUES
('Анна Смирнова', '+79000000001'),
('Иван Петров', '+79000000002'),
('Мария Иванова', '+79000000003'),
('Алексей Соколов', '+79000000004'),
('Ольга Волкова', '+79000000005');
INSERT INTO restaurant_orders (customer_id, table_number, description, total, status, created_at) VALUES
((SELECT id FROM customers WHERE phone='+79000000001'),1,'Пицца Маргарита, чай',850.00,'NEW','2026-09-20 12:00'),
((SELECT id FROM customers WHERE phone='+79000000002'),2,'Борщ, хлеб, морс',620.00,'COOKING','2026-09-20 12:10'),
((SELECT id FROM customers WHERE phone='+79000000003'),3,'Салат Цезарь, кофе',780.00,'SERVED','2026-09-20 12:20'),
((SELECT id FROM customers WHERE phone='+79000000004'),4,'Стейк, картофель, чай',1800.00,'PAID','2026-09-20 12:30'),
((SELECT id FROM customers WHERE phone='+79000000005'),5,'Паста Карбонара, сок',950.00,'CANCELLED','2026-09-20 12:40'),
((SELECT id FROM customers WHERE phone='+79000000001'),6,'Суп грибной, салат',700.00,'PAID','2026-09-21 13:00'),
((SELECT id FROM customers WHERE phone='+79000000002'),7,'Пицца Пепперони, морс',1100.00,'NEW','2026-09-21 13:10'),
((SELECT id FROM customers WHERE phone='+79000000003'),8,'Рыба, рис, чай',1300.00,'COOKING','2026-09-21 13:20'),
((SELECT id FROM customers WHERE phone='+79000000004'),9,'Чизкейк, кофе',550.00,'PAID','2026-09-21 13:30'),
((SELECT id FROM customers WHERE phone='+79000000005'),10,'Овощной салат, вода',400.00,'CANCELLED','2026-09-21 13:40');
COMMIT;
