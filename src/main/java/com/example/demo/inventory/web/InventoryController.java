package com.example.demo.inventory.web;

import com.example.demo.inventory.application.InventoryService;
import com.example.demo.inventory.application.RestockCommand;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
class InventoryController {

  private final InventoryService service;

  InventoryController(InventoryService service) {
    this.service = service;
  }

  @GetMapping("/{productId}")
  ResponseEntity<InventoryStockDto> stock(@PathVariable UUID productId) {
    return ResponseEntity.ok(new InventoryStockDto(productId, service.stockFor(productId)));
  }

  @PostMapping("/{productId}/restock")
  ResponseEntity<InventoryStockDto> restock(
      @PathVariable UUID productId, @RequestBody RestockRequest request) {
    var view = service.restock(new RestockCommand(productId, request.amount()));
    return ResponseEntity.ok(new InventoryStockDto(view.productId(), view.stock()));
  }
}
