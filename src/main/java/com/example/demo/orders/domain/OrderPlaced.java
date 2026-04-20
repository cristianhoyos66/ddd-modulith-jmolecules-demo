package com.example.demo.orders.domain;

import com.example.demo.catalog.domain.Product;
import com.example.demo.customers.domain.Customer;
import java.util.List;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record OrderPlaced(
    Order.OrderIdentifier orderId, Customer.CustomerIdentifier customerId, List<Line> lines) {

  public record Line(Product.ProductIdentifier productId, long quantity) {}
}
