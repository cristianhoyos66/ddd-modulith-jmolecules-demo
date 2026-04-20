package com.example.demo.inventory.application;

import com.example.demo.catalog.domain.Product;
import com.example.demo.inventory.domain.InventoryItem;
import com.example.demo.inventory.domain.InventoryItemRepository;
import java.util.UUID;
import org.jmolecules.ddd.annotation.Service;
import org.jmolecules.ddd.types.Association;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private final InventoryItemRepository repository;

  public InventoryService(InventoryItemRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public InventoryItemView restock(RestockCommand command) {
    var productId = new Product.ProductIdentifier(command.productId());
    var item =
        repository
            .findByProduct(Association.forId(productId))
            .orElseThrow(
                () -> new IllegalStateException("No inventory item for product " + productId));
    var updated = repository.save(item.refillBy(command.amount()));
    return InventoryItemView.from(updated);
  }

  public long stockFor(UUID productId) {
    return repository
        .findByProduct(Association.forId(new Product.ProductIdentifier(productId)))
        .map(InventoryItem::getStock)
        .orElse(0L);
  }

  /** Invoked from listener on ProductAdded. Joins the listener's transaction. */
  public void track(Product.ProductIdentifier productId) {
    repository.save(new InventoryItem(productId));
  }

  /** Invoked from listener on OrderPlaced — atomic find-then-save. */
  @Transactional
  public void withdraw(Product.ProductIdentifier productId, long amount) {
    var item =
        repository
            .findByProduct(Association.forId(productId))
            .orElseThrow(
                () -> new IllegalStateException("No inventory item for product " + productId));
    repository.save(item.reduceBy(amount));
  }
}
