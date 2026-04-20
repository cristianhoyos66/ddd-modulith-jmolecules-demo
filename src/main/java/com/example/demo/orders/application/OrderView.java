package com.example.demo.orders.application;

import com.example.demo.orders.domain.Order;
import java.util.List;
import java.util.UUID;

/** Application-layer read model returned by OrderService. */
public record OrderView(UUID id, UUID customerId, String status, List<LineView> lines) {

  public record LineView(UUID productId, long quantity) {}

  static OrderView from(Order order) {
    var lines =
        order.getLineItems().stream()
            .map(li -> new LineView(li.getProduct().getId().id(), li.getQuantity()))
            .toList();
    return new OrderView(
        order.getId().id(), order.getCustomer().getId().id(), order.getStatus().name(), lines);
  }
}
