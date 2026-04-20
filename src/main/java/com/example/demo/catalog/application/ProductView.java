package com.example.demo.catalog.application;

import com.example.demo.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.UUID;

/** Application-layer read model returned by CatalogService. */
public record ProductView(UUID id, String name, BigDecimal price, String currency) {

  static ProductView from(Product product) {
    return new ProductView(
        product.getId().id(),
        product.getName(),
        product.getPrice().amount(),
        product.getPrice().currency().getCurrencyCode());
  }
}
