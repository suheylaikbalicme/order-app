package com.emar.order_app.admin;

import com.emar.order_app.customer.CustomerEntity;
import com.emar.order_app.customer.CustomerRepository;
import com.emar.order_app.order.OrderEntity;
import com.emar.order_app.order.OrderRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AdminExportController {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public AdminExportController(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/admin/export/customers.csv")
    public void exportCustomersCsv(HttpServletResponse response) throws IOException {
        List<CustomerEntity> list = customerRepository.findAllByOrderByIdDesc();

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=customers.csv");

        StringBuilder sb = new StringBuilder();
        sb.append("id,customerCode,customerName,phone,email,address,syncStatus,lastSyncAt\n");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        for (CustomerEntity c : list) {
            sb.append(c.getId()).append(',')
                    .append(csv(c.getCustomerCode())).append(',')
                    .append(csv(c.getCustomerName())).append(',')
                    .append(csv(c.getPhone())).append(',')
                    .append(csv(c.getEmail())).append(',')
                    .append(csv(c.getAddress())).append(',')
                    .append(csv(c.getSyncStatus() != null ? c.getSyncStatus().name() : ""))
                    .append(',')
                    .append(csv(c.getLastSyncAt() != null ? fmt.format(c.getLastSyncAt()) : ""))
                    .append('\n');
        }

        response.getWriter().write(sb.toString());
    }

    @GetMapping("/admin/export/orders.csv")
    public void exportOrdersCsv(HttpServletResponse response) throws IOException {
        List<OrderEntity> list = orderRepository.findAllByOrderByIdDesc();

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=orders.csv");

        StringBuilder sb = new StringBuilder();
        sb.append("id,orderDate,customerCode,customerName,status,subtotalAmount,discountTotal,vatTotal,grandTotal,currency,exchangeRate,vatRate,syncStatus,lastSyncAt\n");
        DateTimeFormatter dt = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter odt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        for (OrderEntity o : list) {
            sb.append(o.getId()).append(',')
                    .append(csv(o.getOrderDate() != null ? dt.format(o.getOrderDate()) : "")).append(',')
                    .append(csv(o.getCustomerCode())).append(',')
                    .append(csv(o.getCustomerName())).append(',')
                    .append(csv(o.getStatus() != null ? o.getStatus().name() : "")).append(',')
                    .append(num(o.getSubtotalAmount())).append(',')
                    .append(num(o.getDiscountTotal())).append(',')
                    .append(num(o.getVatTotal())).append(',')
                    .append(num(o.getGrandTotal())).append(',')
                    .append(csv(o.getCurrency())).append(',')
                    .append(num(o.getExchangeRate())).append(',')
                    .append(num(o.getVatRate())).append(',')
                    .append(csv(o.getSyncStatus() != null ? o.getSyncStatus().name() : "")).append(',')
                    .append(csv(o.getLastSyncAt() != null ? odt.format(o.getLastSyncAt()) : ""))
                    .append('\n');
        }

        response.getWriter().write(sb.toString());
    }

    private static String num(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }

    private static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\r", " ").replace("\n", " ");
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains(";");
        s = s.replace("\"", "\"\"");
        return needsQuote ? '"' + s + '"' : s;
    }
}
