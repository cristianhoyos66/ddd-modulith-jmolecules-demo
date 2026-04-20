package com.example.demo.catalog.web;

import com.example.demo.catalog.application.AddProductCommand;
import com.example.demo.catalog.application.CatalogService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
class CatalogController {

  private final CatalogService service;

  CatalogController(CatalogService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<ProductDto> add(@RequestBody AddProductRequest request) {
    var view =
        service.add(new AddProductCommand(request.name(), request.price(), request.currency()));
    return ResponseEntity.ok(ProductDto.from(view));
  }

  @GetMapping("/{id}")
  ResponseEntity<ProductDto> get(@PathVariable UUID id) {
    return ResponseEntity.ok(ProductDto.from(service.get(id)));
  }
}
