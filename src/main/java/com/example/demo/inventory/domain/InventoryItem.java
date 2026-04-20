package com.example.demo.inventory.domain;

import com.example.demo.catalog.domain.Product;
import java.util.UUID;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.data.domain.AbstractAggregateRoot;

public class InventoryItem extends AbstractAggregateRoot<InventoryItem>
    implements AggregateRoot<InventoryItem, InventoryItem.InventoryItemIdentifier> {

  private final InventoryItemIdentifier id;
  private final Association<Product, Product.ProductIdentifier> product;
  private long stock;

  public InventoryItem(Product.ProductIdentifier productId) {
    this.id = new InventoryItemIdentifier(UUID.randomUUID());
    this.product = Association.forId(productId);
    this.stock = 0;
  }

  public InventoryItem refillBy(long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Refill amount must be positive");
    }
    this.stock += amount;
    return this;
  }

  public InventoryItem reduceBy(long amount) {
    if (amount > stock) {
      throw new InsufficientStock(product.getId(), stock, amount);
    }
    this.stock -= amount;
    if (stock == 0) {
      registerEvent(new OutOfStock(product));
    }
    return this;
  }

  @Override
  public InventoryItemIdentifier getId() {
    return id;
  }

  public Association<Product, Product.ProductIdentifier> getProduct() {
    return product;
  }

  public long getStock() {
    return stock;
  }

  public record InventoryItemIdentifier(UUID id) implements Identifier {}
}
