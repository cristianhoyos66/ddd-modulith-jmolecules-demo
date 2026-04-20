package com.example.demo.orders.web;

import com.example.demo.orders.application.OrderTotalView;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderTotalDto(UUID orderId, BigDecimal total, String currency) {

  static OrderTotalDto from(OrderTotalView view) {
    return new OrderTotalDto(view.orderId(), view.total(), view.currency());
  }
}
