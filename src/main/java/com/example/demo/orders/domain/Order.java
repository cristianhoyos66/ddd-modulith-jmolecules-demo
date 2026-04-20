package com.example.demo.orders.domain;

import com.example.demo.catalog.domain.Product;
import com.example.demo.customers.domain.Customer;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.data.domain.AbstractAggregateRoot;

@Table(name = "orders")
public class Order extends AbstractAggregateRoot<Order>
    implements AggregateRoot<Order, Order.OrderIdentifier> {

  private OrderIdentifier id;
  private Association<Customer, Customer.CustomerIdentifier> customer;
  private List<LineItem> lineItems = new ArrayList<>();
  private Status status;

  Order() {}

  public Order(Customer.CustomerIdentifier customerId) {
    this.id = new OrderIdentifier(UUID.randomUUID());
    this.customer = Association.forId(customerId);
    this.status = Status.DRAFT;
  }

  public Order addLine(Product.ProductIdentifier productId, long quantity) {
    if (status != Status.DRAFT) {
      throw new IllegalStateException("Cannot modify a placed order");
    }
    lineItems.add(new LineItem(productId, quantity));
    return this;
  }

  public Order place() {
    if (lineItems.isEmpty()) {
      throw new IllegalStateException("Order has no line items");
    }
    this.status = Status.PLACED;
    var lines =
        lineItems.stream()
            .map(li -> new OrderPlaced.Line(li.getProduct().getId(), li.getQuantity()))
            .toList();
    registerEvent(new OrderPlaced(id, customer.getId(), lines));
    return this;
  }

  public Order complete() {
    if (status != Status.PLACED) {
      throw new IllegalStateException("Only placed orders can complete");
    }
    this.status = Status.COMPLETED;
    registerEvent(new OrderCompleted(id));
    return this;
  }

  @Override
  public OrderIdentifier getId() {
    return id;
  }

  public Association<Customer, Customer.CustomerIdentifier> getCustomer() {
    return customer;
  }

  public List<LineItem> getLineItems() {
    return Collections.unmodifiableList(lineItems);
  }

  public Status getStatus() {
    return status;
  }

  public enum Status {
    DRAFT,
    PLACED,
    COMPLETED
  }

  public record OrderIdentifier(UUID id) implements Identifier {}
}
