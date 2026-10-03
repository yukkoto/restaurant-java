package ru.mirea.restaurant.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.restaurant.model.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelExporter {
    public void export(Path path, List<Customer> customers, List<RestaurantOrder> orders) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = workbook.createCellStyle();
            header.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy hh:mm"));
            CellStyle wrap = workbook.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet orderSheet = workbook.createSheet("Заказы");
            addHeader(orderSheet, header, "ID", "ID клиента", "Стол", "Состав заказа", "Сумма, руб.", "Статус", "Создан");
            int index = 1;
            for (RestaurantOrder order : orders) {
                Row row = orderSheet.createRow(index++);
                row.createCell(0).setCellValue(order.getId());
                row.createCell(1).setCellValue(order.getCustomerId());
                row.createCell(2).setCellValue(order.getTableNumber());
                row.createCell(3).setCellValue(order.getDescription());
                row.getCell(3).setCellStyle(wrap);
                row.createCell(4).setCellValue(order.getTotal().doubleValue());
                row.getCell(4).setCellStyle(money);
                row.createCell(5).setCellValue(order.getStatus().getTitle());
                row.createCell(6).setCellValue(order.getCreatedAt());
                row.getCell(6).setCellStyle(date);
                row.setHeightInPoints(18 * (1 + order.getDescription().length() / 45));
            }
            finish(orderSheet, new int[]{8, 14, 9, 52, 18, 18, 23});

            Sheet customerSheet = workbook.createSheet("Клиенты");
            addHeader(customerSheet, header, "ID", "Имя", "Телефон");
            index = 1;
            for (Customer customer : customers) {
                Row row = customerSheet.createRow(index++);
                row.createCell(0).setCellValue(customer.getId());
                row.createCell(1).setCellValue(customer.getName());
                row.getCell(1).setCellStyle(wrap);
                row.createCell(2).setCellValue(customer.getPhone());
                row.setHeightInPoints(18 * (1 + customer.getName().length() / 30));
            }
            finish(customerSheet, new int[]{8, 36, 22});
            try (OutputStream output = Files.newOutputStream(path)) { workbook.write(output); }
        }
    }
    private void addHeader(Sheet sheet, CellStyle style, String... titles) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(26);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(style);
        }
    }
    private void finish(Sheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, widths.length - 1));
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setFitToPage(true);
        sheet.setRepeatingRows(new CellRangeAddress(0, 0, -1, -1));
    }
}
