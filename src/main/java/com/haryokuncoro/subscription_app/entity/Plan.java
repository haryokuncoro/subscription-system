package com.haryokuncoro.subscription_app.entity;

import com.haryokuncoro.subscription_app.dto.enums.BillingInterval;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "stripe_product_id", nullable = false, unique = true)
    private String stripeProductId;

    @Column(name = "stripe_price_id", nullable = false, unique = true)
    private String stripePriceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 5)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    private BillingInterval billingInterval;

    @Column(nullable = false)
    private boolean active = true;

}