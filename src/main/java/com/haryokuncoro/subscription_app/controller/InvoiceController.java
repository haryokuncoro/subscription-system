package com.haryokuncoro.subscription_app.controller;

import com.haryokuncoro.subscription_app.dto.ApiResponse;
import com.haryokuncoro.subscription_app.dto.GetInvoiceResponse;
import com.haryokuncoro.subscription_app.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @GetMapping
    public ApiResponse<Page<GetInvoiceResponse>> getInvoices(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(required = false) String stripeInvoiceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<GetInvoiceResponse> resp = invoiceService.search(
                userId,
                subscriptionId,
                stripeInvoiceId,
                status,
                pageable);
        return ApiResponse.success(resp);
    }

}
