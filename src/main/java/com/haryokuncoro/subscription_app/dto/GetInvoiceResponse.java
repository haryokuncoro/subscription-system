package com.haryokuncoro.subscription_app.dto;

import com.haryokuncoro.subscription_app.dto.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class GetInvoiceResponse {
    private UUID userId;

    private UUID subscriptionId;

    private UUID planId;

    private String stripeInvoiceId;

    private String invoiceNumber;

    private InvoiceStatus status;

    private BigDecimal subtotal;

    private BigDecimal tax;

    private BigDecimal total;

    private BigDecimal amountDue;

    private BigDecimal amountPaid;

    private String currency;

    private String hostedInvoiceUrl;

    private String invoicePdf;

    private Instant periodStart;

    private Instant periodEnd;

    private Instant paidAt;
}
