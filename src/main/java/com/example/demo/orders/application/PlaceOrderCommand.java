package com.example.demo.orders.application;

import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(UUID customerId, List<Line> lines) {

  public record Line(UUID productId, long quantity) {}
}
