package com.example.demo.catalog.domain;

import org.springframework.data.repository.CrudRepository;

public interface CatalogRepository extends CrudRepository<Product, Product.ProductIdentifier> {}
