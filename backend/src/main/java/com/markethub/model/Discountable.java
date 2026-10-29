package com.markethub.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Something sold at a current price that may be lower than its original price.
 * The default methods are shared logic; implementers only supply the two prices.
 */
public interface Discountable {

    /** @return the price a buyer pays today */
    BigDecimal getPrice();

    /** @return the price before the discount, or zero when there is no discount */
    BigDecimal getOldPrice();

    /** @return {@code true} when an old price exists and is higher than the current one */
    default boolean isOnSale() {
        return getOldPrice() != null
                && getOldPrice().signum() > 0
                && getOldPrice().compareTo(getPrice()) > 0;
    }

    /** @return whole-number percentage saved, e.g. 28 for $129 against $179; 0 when not on sale */
    default int getDiscountPercent() {
        if (!isOnSale()) {
            return 0;
        }
        BigDecimal saved = getOldPrice().subtract(getPrice());
        return saved.multiply(BigDecimal.valueOf(100))
                .divide(getOldPrice(), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
