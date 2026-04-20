package com.example.demo.orders.domain;

import com.example.demo.catalog.domain.Product;
import java.util.UUID;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Entity;
import org.jmolecules.ddd.types.Identifier;

public class LineItem implements Entity<Order, LineItem.LineItemIdentifier> {

  private LineItemIdentifier id;
  private Association<Product, Product.ProductIdentifier> product;
  private long quantity;

  LineItem() {}

  LineItem(Product.ProductIdentifier productId, long quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    this.id = new LineItemIdentifier(UUID.randomUUID());
    this.product = Association.forId(productId);
    this.quantity = quantity;
  }

  @Override
  public LineItemIdentifier getId() {
    return id;
  }

  public Association<Product, Product.ProductIdentifier> getProduct() {
    return product;
  }

  public long getQuantity() {
    return quantity;
  }

  public record LineItemIdentifier(UUID id) implements Identifier {}
}
