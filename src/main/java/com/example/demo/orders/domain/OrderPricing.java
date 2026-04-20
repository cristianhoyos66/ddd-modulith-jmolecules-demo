package com.example.demo.orders.domain;

import com.example.demo.catalog.domain.Money;
import com.example.demo.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import org.jmolecules.ddd.annotation.Service;

/**
 * Domain service encapsulating the business rules that price an order.
 *
 * <p>Rules live here rather than on the Order aggregate because they depend on data outside the
 * aggregate (product prices). The service is stateless and does no I/O: the application service
 * fetches prices and hands them in. That keeps the rules testable in isolation and keeps the domain
 * free of repository and framework concerns.
 */
@Service
public class OrderPricing {

  private static final BigDecimal BULK_DISCOUNT_THRESHOLD = new BigDecimal("100");
  private static final BigDecimal BULK_DISCOUNT_RATE = new BigDecimal("0.90");

  public Money totalFor(Order order, Map<Product.ProductIdentifier, Money> prices) {
    var subtotal =
        order.getLineItems().stream()
            .map(line -> lineTotal(line, prices))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    var withDiscount =
        subtotal.compareTo(BULK_DISCOUNT_THRESHOLD) > 0
            ? subtotal.multiply(BULK_DISCOUNT_RATE)
            : subtotal;

    return new Money(withDiscount, currencyFrom(prices));
  }

  private BigDecimal lineTotal(LineItem line, Map<Product.ProductIdentifier, Money> prices) {
    var price = prices.get(line.getProduct().getId());
    if (price == null) {
      throw new IllegalArgumentException("No price for product " + line.getProduct().getId());
    }
    return price.amount().multiply(BigDecimal.valueOf(line.getQuantity()));
  }

  private Currency currencyFrom(Map<Product.ProductIdentifier, Money> prices) {
    return prices.values().stream()
        .map(Money::currency)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No prices provided"));
  }
}
