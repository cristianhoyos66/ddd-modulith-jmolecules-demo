package com.example.demo.orders.web;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(UUID customerId, List<Line> lines) {

  public record Line(UUID productId, long quantity) {}
}
