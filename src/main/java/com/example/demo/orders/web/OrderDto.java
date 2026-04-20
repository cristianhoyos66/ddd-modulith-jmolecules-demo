package com.example.demo.orders.web;

import com.example.demo.orders.application.OrderView;
import java.util.List;
import java.util.UUID;

public record OrderDto(UUID id, UUID customerId, String status, List<LineDto> lines) {

  public record LineDto(UUID productId, long quantity) {}

  static OrderDto from(OrderView view) {
    var lines = view.lines().stream().map(l -> new LineDto(l.productId(), l.quantity())).toList();
    return new OrderDto(view.id(), view.customerId(), view.status(), lines);
  }
}
