package com.example.demo.inventory.infrastructure;

import com.example.demo.catalog.domain.ProductAdded;
import com.example.demo.inventory.application.InventoryService;
import com.example.demo.orders.domain.OrderPlaced;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter: translates domain events from other modules into inventory operations.
 *
 * <p>Lives in infrastructure because it wires the module to the external event stream. The
 * application service stays unaware of the event topology.
 */
@Component
class InventoryListener {

  private final InventoryService inventory;

  InventoryListener(InventoryService inventory) {
    this.inventory = inventory;
  }

  @ApplicationModuleListener
  void on(ProductAdded event) {
    inventory.track(event.productId());
  }

  @ApplicationModuleListener
  void on(OrderPlaced event) {
    event.lines().forEach(line -> inventory.withdraw(line.productId(), line.quantity()));
  }
}
