package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.enums.InvoiceStatus;
import com.haryokuncoro.subscription_app.entity.Invoice;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.InvoiceRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;


    @Transactional
    public void syncFromStripeEvent(Event event) {
        com.stripe.model.Invoice stripeInvoice =
                (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new IllegalStateException("Could not deserialize invoice"));

        String stripeSubscriptionId = extractSubscriptionId(stripeInvoice);

        if (stripeSubscriptionId == null) {
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new NotFoundException(
                        "Subscription not found for Stripe subscription: " + stripeSubscriptionId));

        Invoice invoice = invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId())
                .orElseGet(() -> Invoice.builder()
                        .subscription(subscription)
                        .stripeInvoiceId(stripeInvoice.getId())
                        .build());

        invoice.setInvoiceNumber(stripeInvoice.getNumber());
        invoice.setStatus(mapInvoiceStatus(stripeInvoice.getStatus()));
        invoice.setTotal(stripeInvoice.getTotal());
        invoice.setSubtotal(stripeInvoice.getSubtotal());
        invoice.setTax(getTax(stripeInvoice));
        invoice.setAmountDue(stripeInvoice.getAmountDue());
        invoice.setAmountPaid(stripeInvoice.getAmountPaid());
        invoice.setCurrency(stripeInvoice.getCurrency().toUpperCase());
        invoice.setHostedInvoiceUrl(stripeInvoice.getHostedInvoiceUrl());
        invoice.setInvoicePdf(stripeInvoice.getInvoicePdf());

        Long periodStart = extractPeriodStart(stripeInvoice);
        Long periodEnd = extractPeriodEnd(stripeInvoice);
        if (periodStart != null) invoice.setPeriodStart(Instant.ofEpochSecond(periodStart));
        if (periodEnd != null) invoice.setPeriodEnd(Instant.ofEpochSecond(periodEnd));

        if ("paid".equals(stripeInvoice.getStatus())) {
            invoice.setPaidAt(Instant.now());
        }

        invoiceRepository.save(invoice);
    }

    private Long getTax(com.stripe.model.Invoice stripeInvoice){
        List<com.stripe.model.Invoice.TotalTax> taxes = stripeInvoice.getTotalTaxes();
        Long total = 0L;
        for(com.stripe.model.Invoice.TotalTax tax: taxes){
            total += tax.getAmount();
        }
        return total;
    }

    private String extractSubscriptionId(com.stripe.model.Invoice invoice) {
        if (invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        return invoice.getParent().getSubscriptionDetails().getSubscription();
    }

    private Long extractPeriodStart(com.stripe.model.Invoice invoice) {
        if (invoice.getLines() == null || invoice.getLines().getData().isEmpty()) {
            return null;
        }
        return invoice.getLines().getData().get(0).getPeriod().getStart();
    }

    private Long extractPeriodEnd(com.stripe.model.Invoice invoice) {
        if (invoice.getLines() == null || invoice.getLines().getData().isEmpty()) {
            return null;
        }
        return invoice.getLines().getData().get(0).getPeriod().getEnd();
    }


    private InvoiceStatus mapInvoiceStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "draft" -> InvoiceStatus.DRAFT;
            case "open" -> InvoiceStatus.OPEN;
            case "paid" -> InvoiceStatus.PAID;
            case "uncollectible" -> InvoiceStatus.UNCOLLECTIBLE;
            case "void" -> InvoiceStatus.VOID;
            default -> throw new IllegalArgumentException("Unknown invoice status: " + stripeStatus);
        };
    }
}