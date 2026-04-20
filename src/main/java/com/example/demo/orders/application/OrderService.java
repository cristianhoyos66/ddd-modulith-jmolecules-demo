package com.example.demo.orders.application;

import com.example.demo.catalog.application.CatalogService;
import com.example.demo.catalog.domain.Money;
import com.example.demo.catalog.domain.Product;
import com.example.demo.customers.application.CustomerService;
import com.example.demo.customers.domain.Customer;
import com.example.demo.orders.domain.Order;
import com.example.demo.orders.domain.OrderPricing;
import com.example.demo.orders.domain.OrderRepository;
import com.example.demo.orders.domain.UnknownCustomer;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private final OrderRepository orders;
  private final CustomerService customers;
  private final CatalogService catalog;
  private final OrderPricing pricing;

  public OrderService(
      OrderRepository orders,
      CustomerService customers,
      CatalogService catalog,
      OrderPricing pricing) {
    this.orders = orders;
    this.customers = customers;
    this.catalog = catalog;
    this.pricing = pricing;
  }

  @Transactional
  public OrderView place(PlaceOrderCommand command) {
    var customerId = new Customer.CustomerIdentifier(command.customerId());
    if (!customers.exists(customerId)) {
      throw new UnknownCustomer(customerId);
    }
    var order = new Order(customerId);
    command
        .lines()
        .forEach(
            line ->
                order.addLine(new Product.ProductIdentifier(line.productId()), line.quantity()));
    return OrderView.from(orders.save(order.place()));
  }

  @Transactional
  public OrderView complete(CompleteOrderCommand command) {
    var id = new Order.OrderIdentifier(command.orderId());
    var order =
        orders.findById(id).orElseThrow(() -> new IllegalArgumentException("No order " + id));
    return OrderView.from(orders.save(order.complete()));
  }

  @Transactional(readOnly = true)
  public OrderView get(UUID id) {
    var orderId = new Order.OrderIdentifier(id);
    return OrderView.from(
        orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("No order " + id)));
  }

  /**
   * Orchestration use case: fetch prices from the catalog module, hand everything to the
   * OrderPricing domain service, return the total. The application service does the I/O; the domain
   * service does the math.
   */
  @Transactional(readOnly = true)
  public OrderTotalView calculateTotal(UUID id) {
    var orderId = new Order.OrderIdentifier(id);
    var order =
        orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("No order " + id));

    Map<Product.ProductIdentifier, Money> prices =
        order.getLineItems().stream()
            .map(line -> line.getProduct().getId())
            .distinct()
            .collect(Collectors.toMap(Function.identity(), catalog::priceOf));

    var total = pricing.totalFor(order, prices);
    return new OrderTotalView(
        order.getId().id(), total.amount(), total.currency().getCurrencyCode());
  }
}
