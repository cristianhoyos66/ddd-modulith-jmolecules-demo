package com.example.demo.inventory.domain;

import com.example.demo.catalog.domain.Product;
import java.util.Optional;
import org.jmolecules.ddd.types.Association;
import org.springframework.data.repository.CrudRepository;

public interface InventoryItemRepository
    extends CrudRepository<InventoryItem, InventoryItem.InventoryItemIdentifier> {

  Optional<InventoryItem> findByProduct(Association<Product, Product.ProductIdentifier> product);
}
