package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.demo.catalog.application.AddProductCommand;
import com.example.demo.catalog.application.CatalogService;
import com.example.demo.customers.application.CustomerService;
import com.example.demo.customers.application.RegisterCustomerCommand;
import com.example.demo.inventory.application.InventoryService;
import com.example.demo.inventory.application.RestockCommand;
import com.example.demo.orders.application.OrderService;
import com.example.demo.orders.application.PlaceOrderCommand;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-stack scenario demonstrating event-driven cross-module communication.
 *
 * <ol>
 *   <li>Register a customer → orders materializes a local CustomerLookup via CustomerRegistered.
 *   <li>Add a product → inventory tracks it with zero stock via ProductAdded.
 *   <li>Restock → inventory has stock available.
 *   <li>Place an order → inventory reacts to OrderPlaced and decrements stock.
 * </ol>
 *
 * <p>No module calls another directly — everything flows via domain events.
 */
@SpringBootTest
class EndToEndOrderFlowTests {

  @Autowired CustomerService customers;
  @Autowired CatalogService catalog;
  @Autowired InventoryService inventory;
  @Autowired OrderService orders;

  @Test
  void endToEndOrderFlow() {
    var customer = customers.register(new RegisterCustomerCommand("Alice", "alice@example.com"));
    var product = catalog.add(new AddProductCommand("Widget", "9.99", "USD"));

    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(() -> assertThat(inventory.stockFor(product.id())).isEqualTo(0L));

    inventory.restock(new RestockCommand(product.id(), 10));
    assertThat(inventory.stockFor(product.id())).isEqualTo(10L);

    orders.place(
        new PlaceOrderCommand(customer.id(), List.of(new PlaceOrderCommand.Line(product.id(), 3))));

    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(() -> assertThat(inventory.stockFor(product.id())).isEqualTo(7L));
  }
}
