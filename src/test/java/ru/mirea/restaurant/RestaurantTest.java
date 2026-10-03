package ru.mirea.restaurant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.restaurant.exception.*;
import ru.mirea.restaurant.model.*;
import ru.mirea.restaurant.repository.*;
import ru.mirea.restaurant.service.*;
import ru.mirea.restaurant.util.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// Использовать ТОЛЬКО отдельную пустую базу restaurant_test: перед каждым тестом данные очищаются.
@EnabledIfEnvironmentVariable(named = "RESTAURANT_TEST_CONFIG", matches = ".+")
public class RestaurantTest {
    private DatabaseManager database;
    private CustomerRepository customerRepository;
    private OrderRepository orderRepository;
    private CustomerService customers;
    private OrderService orders;
    @TempDir Path temp;

    @BeforeEach
    void prepare() throws Exception {
        database = new DatabaseManager(Path.of(System.getenv("RESTAURANT_TEST_CONFIG")));
        try (Connection c = database.getConnection()) {
            assertEquals("restaurant_test", c.getCatalog(), "Тесты разрешены только в базе restaurant_test!");
            execute(c, Files.readString(Path.of("sql/01_schema.sql")));
            execute(c, "TRUNCATE restaurant_orders, customers RESTART IDENTITY CASCADE;");
            execute(c, Files.readString(Path.of("sql/02_seed.sql")));
        }
        customerRepository = new CustomerRepository(database);
        orderRepository = new OrderRepository(database);
        customers = new CustomerService(customerRepository, orderRepository);
        orders = new OrderService(orderRepository, customerRepository);
    }
    private void execute(Connection c, String script) throws SQLException {
        for (String sql : script.split(";")) {
            if (!sql.isBlank()) try (PreparedStatement ps = c.prepareStatement(sql)) { ps.execute(); }
        }
    }
    private RestaurantOrder create() {
        return orders.create(1, 12, "Суп, чай", new BigDecimal("500.25"));
    }
    @Test void seedAndStatistics() {
        assertEquals(5, customers.findAll().size());
        assertEquals(10, orders.findAll().size());
        assertEquals("3050.00", orders.statistics().get("Выручка по оплаченным заказам, руб."));
        assertEquals("3", orders.statistics().get("Оплачен"));
        assertEquals(8, orders.statistics().size());
    }
    @Test void customerCrud() {
        Customer c = customers.create("Новый клиент", "+79000000009");
        customers.update(c.getId(), "Другое имя", "+79000000008");
        assertEquals("Другое имя", customers.findById(c.getId()).getName());
        customers.delete(c.getId());
        assertThrows(EntityNotFoundException.class, () -> customers.findById(c.getId()));
    }
    @Test void orderCrudAndPersistence() {
        RestaurantOrder o = create();
        assertEquals(OrderStatus.NEW, o.getStatus());
        assertNotNull(o.getCreatedAt());
        orders.update(o.getId(), 2, 13, "Рыба", new BigDecimal("700.10"));
        RestaurantOrder reloaded = new OrderRepository(database).findById(o.getId());
        assertEquals("Рыба", reloaded.getDescription());
        assertEquals(new BigDecimal("700.10"), reloaded.getTotal());
        assertEquals(o.getCreatedAt(), reloaded.getCreatedAt());
        orders.delete(o.getId());
        assertThrows(EntityNotFoundException.class, () -> orders.findById(o.getId()));
    }
    @Test void requiredFieldsAndPhone() {
        assertThrows(BusinessException.class, () -> customers.create(" ", "+79000000009"));
        assertThrows(BusinessException.class, () -> customers.create("Анна", "123"));
        assertThrows(BusinessException.class, () -> customers.create("Анна", "+79000000001"));
        assertThrows(BusinessException.class, () -> orders.create(1, 1, " ", BigDecimal.ONE));
        assertThrows(BusinessException.class, () -> orders.create(1, 1, "x".repeat(501), BigDecimal.ONE));
    }
    @Test void moneyAndTableBoundaries() {
        for (int table : new int[]{0, 21})
            assertThrows(BusinessException.class, () -> orders.create(1, table, "Чай", BigDecimal.ONE));
        for (String money : new String[]{"0", "-1", "1.001", "100000000.00"})
            assertThrows(BusinessException.class, () -> orders.create(1, 1, "Чай", new BigDecimal(money)));
        assertNotNull(orders.create(1, 20, "Чай", new BigDecimal("0.01")));
        assertNotNull(orders.create(1, 1, "Банкет", new BigDecimal("99999999.99")));
    }
    @Test void missingCustomerAndRecord() {
        assertThrows(EntityNotFoundException.class, () -> orders.create(9999, 1, "Чай", BigDecimal.ONE));
        assertThrows(EntityNotFoundException.class, () -> orders.findById(9999));
        assertThrows(EntityNotFoundException.class, () -> customers.update(9999, "Имя", "+79000000009"));
    }
    @Test void cannotDeleteCustomerWithOrders() {
        assertThrows(BusinessException.class, () -> customers.delete(1));
        assertThrows(DataAccessException.class, () -> customerRepository.delete(1));
    }
    @Test void completeOrderLifecycle() {
        int id = create().getId();
        orders.changeStatus(id, OrderStatus.COOKING);
        orders.changeStatus(id, OrderStatus.SERVED);
        orders.changeStatus(id, OrderStatus.PAID);
        assertEquals(OrderStatus.PAID, orders.findById(id).getStatus());
        assertThrows(BusinessException.class, () -> orders.changeStatus(id, OrderStatus.NEW));
        assertThrows(BusinessException.class, () -> orders.delete(id));
    }
    @Test void forbiddenStatusTransitionsAndUpdates() {
        assertThrows(BusinessException.class, () -> orders.changeStatus(1, OrderStatus.PAID));
        assertThrows(BusinessException.class, () -> orders.changeStatus(1, null));
        assertThrows(BusinessException.class, () -> orders.changeStatus(3, OrderStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> orders.update(2, 1, 1, "Чай", BigDecimal.ONE));
        assertThrows(BusinessException.class, () -> orders.delete(2));
    }
    @Test void cancelAndDelete() {
        orders.changeStatus(1, OrderStatus.CANCELLED);
        assertThrows(BusinessException.class, () -> orders.changeStatus(1, OrderStatus.NEW));
        orders.delete(1);
        orders.changeStatus(2, OrderStatus.CANCELLED);
        orders.delete(2);
        assertEquals(8, orders.findAll().size());
    }
    @Test void searchFiltersSorting() {
        assertEquals(2, orders.searchByDescription("ПИЦЦА").size());
        assertEquals(2, orders.searchByCustomerName("смирнова").size());
        assertEquals(0, orders.searchByDescription("несуществующее блюдо").size());
        assertEquals(3, orders.filterByStatus(OrderStatus.PAID).size());
        assertEquals(1, orders.filterByTable(1).size());
        assertEquals(5, orders.filterByDates(LocalDate.of(2026,9,20), LocalDate.of(2026,9,20)).size());
        assertThrows(BusinessException.class, () -> orders.filterByDates(LocalDate.of(2026,9,21), LocalDate.of(2026,9,20)));
        assertEquals(new BigDecimal("400.00"), orders.sortByTotal().get(0).getTotal());
        assertEquals(1, orders.sortByDate().get(0).getId());
        assertEquals(10, orders.sortByDate().get(9).getId());
    }
    @Test void sqlInjectionIsPlainText() {
        String value = "Чай'); DROP TABLE customers; --";
        RestaurantOrder o = orders.create(1, 1, value, BigDecimal.ONE);
        assertEquals(value, orders.findById(o.getId()).getDescription());
        assertEquals(5, customers.findAll().size());
    }
    @Test void databaseConstraints() throws Exception {
        try (Connection c = database.getConnection()) {
            assertThrows(SQLException.class, () -> execute(c, "INSERT INTO customers(name,phone) VALUES ('Дубль','+79000000001')"));
            assertThrows(SQLException.class, () -> execute(c, "UPDATE restaurant_orders SET status='WRONG' WHERE id=1"));
            assertThrows(SQLException.class, () -> execute(c, "UPDATE restaurant_orders SET total=-1 WHERE id=1"));
        }
        assertEquals(List.of("customers", "restaurant_orders"), new DatabaseInfoRepository(database).findTableNames());
    }
    @Test void excelContainsDatabaseRecords() throws Exception {
        Path file = temp.resolve("export.xlsx");
        new ExcelExporter().export(file, customers.findAll(), orders.findAll());
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file))) {
            assertEquals(2, workbook.getNumberOfSheets());
            var sheet = workbook.getSheet("Заказы");
            assertEquals(10, sheet.getLastRowNum());
            assertEquals(850.0, sheet.getRow(1).getCell(4).getNumericCellValue());
            assertTrue(org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(sheet.getRow(1).getCell(6)));
            assertEquals("+79000000001", workbook.getSheet("Клиенты").getRow(1).getCell(2).getStringCellValue());
        }
    }
    @Test void failedConnectionIsReported() throws Exception {
        Path config = temp.resolve("bad.properties");
        Files.writeString(config, "url=jdbc:postgresql://127.0.0.1:1/missing?connectTimeout=1\nuser=test\npassword=test\n");
        assertThrows(DataAccessException.class, () -> new OrderRepository(new DatabaseManager(config)).findAll());
    }
    @Test void emptyDatabaseWorks() throws Exception {
        try (Connection c = database.getConnection()) { execute(c, "TRUNCATE restaurant_orders, customers CASCADE"); }
        assertEquals("0", orders.statistics().get("Всего заказов"));
        assertEquals("0.00", orders.statistics().get("Выручка по оплаченным заказам, руб."));
        Path file = temp.resolve("empty.xlsx");
        new ExcelExporter().export(file, customers.findAll(), orders.findAll());
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file))) {
            assertEquals(0, workbook.getSheet("Заказы").getLastRowNum());
        }
    }
}
