package com.example.demo.catalog.application;

import com.example.demo.catalog.domain.CatalogRepository;
import com.example.demo.catalog.domain.Money;
import com.example.demo.catalog.domain.Product;
import java.util.UUID;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

  private final CatalogRepository repository;

  public CatalogService(CatalogRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public ProductView add(AddProductCommand command) {
    var product =
        repository.save(new Product(command.name(), Money.of(command.price(), command.currency())));
    return ProductView.from(product);
  }

  public ProductView get(UUID id) {
    var product =
        repository
            .findById(new Product.ProductIdentifier(id))
            .orElseThrow(() -> new IllegalArgumentException("No product " + id));
    return ProductView.from(product);
  }

  /** Query use case for other modules: look up a product's price by id. */
  public Money priceOf(Product.ProductIdentifier id) {
    return repository
        .findById(id)
        .map(Product::getPrice)
        .orElseThrow(() -> new IllegalArgumentException("No product " + id));
  }
}
