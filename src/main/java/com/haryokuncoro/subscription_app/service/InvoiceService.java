package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.GetInvoiceResponse;
import com.haryokuncoro.subscription_app.dto.enums.InvoiceStatus;
import com.haryokuncoro.subscription_app.dto.spec.InvoiceSpecification;
import com.haryokuncoro.subscription_app.entity.Invoice;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.InvoiceRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import com.haryokuncoro.subscription_app.utils.GeneralUtils;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service @Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Page<GetInvoiceResponse> search(
            UUID userId,
            UUID subscriptionId,
            String stripeInvoiceId,
            String status,
            Pageable pageable) {

        Specification<Invoice> spec = null;

        if (userId != null) {
            spec = Specification.allOf(
                    spec,
                    InvoiceSpecification.hasUser(userId)
            );
        }
        if (subscriptionId != null) {
            spec = Specification.allOf(
                    spec,
                    InvoiceSpecification.hasSubscription(subscriptionId)
            );
        }

        if (stripeInvoiceId != null) {
            spec = Specification.allOf(
                    spec,
                    InvoiceSpecification.hasStripeInvoiceId(stripeInvoiceId)
            );
        }

        if (status != null) {
            spec = Specification.allOf(
                    spec,
                    InvoiceSpecification.hasStatus(status)
            );
        }

        return invoiceRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    public GetInvoiceResponse toResponse(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        return GetInvoiceResponse.builder()
                .userId(subscription.getUser().getId())
                .userName(subscription.getUser().getFullName())
                .subscriptionId(subscription.getId())
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .stripeInvoiceId(invoice.getStripeInvoiceId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subtotal(GeneralUtils.toDollars(invoice.getSubtotal()))
                .total(GeneralUtils.toDollars(invoice.getTotal()))
                .tax(GeneralUtils.toDollars(invoice.getTax()))
                .amountDue(GeneralUtils.toDollars(invoice.getAmountDue()))
                .amountPaid(GeneralUtils.toDollars(invoice.getAmountPaid()))
                .status(invoice.getStatus())
                .build();
    }



    @Transactional
    public void syncFromStripeEvent(Event event) {
        com.stripe.model.Invoice stripeInvoice;
        try {
            stripeInvoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                            .deserializeUnsafe();
        }catch (Exception e){
            log.error("fail to deserialize invoice object", e);
            return;
        }

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
        if(taxes == null) return total;
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