package com.haryokuncoro.subscription_app.utils;


import java.math.BigDecimal;
import java.math.RoundingMode;

public class GeneralUtils {
    private GeneralUtils() {
        // prevent instantiation
    }
    public static long toCents(BigDecimal dollarAmount) {
        return dollarAmount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
    public static BigDecimal toDollars(Long centAmount) {
        return BigDecimal.valueOf(centAmount)
                .divide(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
