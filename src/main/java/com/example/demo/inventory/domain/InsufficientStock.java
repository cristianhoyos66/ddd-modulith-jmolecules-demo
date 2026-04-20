package com.example.demo.inventory.domain;

import com.example.demo.catalog.domain.Product;

public class InsufficientStock extends RuntimeException {

  public InsufficientStock(Product.ProductIdentifier productId, long available, long requested) {
    super(
        "Insufficient stock for product "
            + productId
            + ": available="
            + available
            + ", requested="
            + requested);
  }
}
