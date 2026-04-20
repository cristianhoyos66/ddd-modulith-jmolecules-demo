package com.example.demo.catalog.web;

import com.example.demo.catalog.application.ProductView;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(UUID id, String name, BigDecimal price, String currency) {

  static ProductDto from(ProductView view) {
    return new ProductDto(view.id(), view.name(), view.price(), view.currency());
  }
}
