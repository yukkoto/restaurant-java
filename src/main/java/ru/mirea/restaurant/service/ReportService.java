package ru.mirea.restaurant.service;

import ru.mirea.restaurant.repository.DatabaseInfoRepository;
import ru.mirea.restaurant.util.ExcelExporter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final CustomerService customers;
    private final OrderService orders;
    private final DatabaseInfoRepository databaseInfo;

    public ReportService(CustomerService customers, OrderService orders, DatabaseInfoRepository databaseInfo) {
        this.customers = customers;
        this.orders = orders;
        this.databaseInfo = databaseInfo;
    }
    public List<String> tableNames() { return databaseInfo.findTableNames(); }

    public Path export() throws IOException {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        Path path = Path.of("exports", "restaurant_" + time + ".xlsx");
        new ExcelExporter().export(path, customers.findAll(), orders.findAll());
        return path.toAbsolutePath();
    }
}
