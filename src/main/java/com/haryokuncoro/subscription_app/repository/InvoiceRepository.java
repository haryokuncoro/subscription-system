package com.haryokuncoro.subscription_app.repository;

import com.haryokuncoro.subscription_app.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByStripeInvoiceId(String invoiceId);
}
