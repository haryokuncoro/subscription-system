package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.GetInvoiceResponse;
import com.haryokuncoro.subscription_app.dto.enums.InvoiceStatus;
import com.haryokuncoro.subscription_app.entity.Invoice;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.entity.Subscription;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.NotFoundException;
import com.haryokuncoro.subscription_app.repository.InvoiceRepository;
import com.haryokuncoro.subscription_app.repository.SubscriptionRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @BeforeEach
    void setUpSecurityContext() {
        User currentUser = User.builder().email("current@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void toResponse_mapsInvoiceFieldsCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        User user = User.builder().fullName("Jane Doe").build();
        user.setId(userId);

        Plan plan = Plan.builder().name("Pro Plan").build();
        plan.setId(planId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .build();
        subscription.setId(subscriptionId);

        Instant periodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-02-01T00:00:00Z");
        Instant paidAt = Instant.parse("2026-01-02T00:00:00Z");

        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .stripeInvoiceId("in_test_123")
                .invoiceNumber("INV-001")
                .status(InvoiceStatus.PAID)
                .subtotal(1000L)
                .tax(100L)
                .total(1100L)
                .amountDue(0L)
                .amountPaid(1100L)
                .currency("USD")
                .hostedInvoiceUrl("https://example.com/invoice")
                .invoicePdf("https://example.com/invoice.pdf")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .paidAt(paidAt)
                .build();
        invoice.setId(invoiceId);

        GetInvoiceResponse response = invoiceService.toResponse(invoice);

        assertThat(response.getId()).isEqualTo(invoiceId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUserName()).isEqualTo("Jane Doe");
        assertThat(response.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(response.getPlanId()).isEqualTo(planId);
        assertThat(response.getPlanName()).isEqualTo("Pro Plan");
        assertThat(response.getStripeInvoiceId()).isEqualTo("in_test_123");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.getSubtotal().toPlainString()).isEqualTo("10.00");
        assertThat(response.getTax().toPlainString()).isEqualTo("1.00");
        assertThat(response.getTotal().toPlainString()).isEqualTo("11.00");
        assertThat(response.getAmountPaid().toPlainString()).isEqualTo("11.00");
        assertThat(response.getPeriodStart()).isEqualTo(periodStart);
        assertThat(response.getPeriodEnd()).isEqualTo(periodEnd);
        assertThat(response.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void search_withFilters_mapsRepositoryResultToResponse() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = User.builder().fullName("Test User").build();
        user.setId(userId);

        Plan plan = Plan.builder().name("Starter").build();
        plan.setId(planId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .build();
        subscription.setId(subscriptionId);

        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .stripeInvoiceId("in_001")
                .invoiceNumber("INV-001")
                .status(InvoiceStatus.PAID)
                .subtotal(500L)
                .tax(50L)
                .total(550L)
                .amountDue(0L)
                .amountPaid(550L)
                .currency("USD")
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Invoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
        when(invoiceRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Invoice>>any(), eq(pageable))).thenReturn(page);

        Page<GetInvoiceResponse> result = invoiceService.search(
                userId,
                subscriptionId,
                "in_001",
                "paid",
                pageable
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStripeInvoiceId()).isEqualTo("in_001");
        assertThat(result.getContent().get(0).getTotal().toPlainString()).isEqualTo("5.50");
        verify(invoiceRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Invoice>>any(), eq(pageable));
    }

    @Test
    void syncFromStripeEvent_deserializationFails_doesNotPersist() throws Exception {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.deserializeUnsafe()).thenThrow(new RuntimeException("bad payload"));

        invoiceService.syncFromStripeEvent(event);

        verifyNoInteractions(subscriptionRepository);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void downloadInvoicePdf_invoiceNotFound_throwsNotFoundException() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.downloadInvoicePdf(invoiceId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Invoice not found with id");
    }

    @Test
    void downloadInvoicePdf_invoicePdfMissing_throwsNotFoundException() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().invoicePdf(null).build();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.downloadInvoicePdf(invoiceId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Invoice PDF is not available");
    }

    @Test
    void downloadInvoicePdf_validUrl_returnsPdfBytes() throws IOException {
        byte[] expectedPdf = "fake-pdf-content".getBytes();
        Path tempPdf = Files.createTempFile("invoice-service-test", ".pdf");
        Files.write(tempPdf, expectedPdf);

        try {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = Invoice.builder().invoicePdf(tempPdf.toUri().toString()).build();
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

            byte[] result = invoiceService.downloadInvoicePdf(invoiceId);

            assertThat(result).isEqualTo(expectedPdf);
        } finally {
            Files.deleteIfExists(tempPdf);
        }
    }
}



