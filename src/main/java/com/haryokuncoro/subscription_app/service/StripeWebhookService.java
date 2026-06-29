package com.haryokuncoro.subscription_app.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service @Slf4j
@RequiredArgsConstructor
public class StripeWebhookService {
    @Value("${stripe.skipSignatureCheck}")
    private boolean skipSignatureCheck;
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    private final InvoiceService invoiceService;


    public void handle(String payload, String signature) {

        Event event;
        if (skipSignatureCheck) {
            event = ApiResource.GSON.fromJson(payload, Event.class);
        } else {
            try {
                event = Webhook.constructEvent(payload, signature, webhookSecret);
            } catch (SignatureVerificationException ex) {
                log.error("invalid webhook signature", ex);
                throw new RuntimeException("Invalid webhook signature", ex);
            }
        }

        switch (event.getType()) {
            case "invoice.created",
                 "invoice.finalized",
                 "invoice.paid",
                 "invoice.payment_failed",
                 "invoice.voided" -> {
                invoiceService.syncFromStripeEvent(event);
            }

            default -> {
                log.info("Ignoring event {}", event.getType() );
            }
        }

    }

}