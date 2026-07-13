package com.haryokuncoro.subscription_app.service;

import com.haryokuncoro.subscription_app.dto.PlanRequest;
import com.haryokuncoro.subscription_app.dto.StripePlanResult;
import com.haryokuncoro.subscription_app.dto.enums.BillingInterval;
import com.haryokuncoro.subscription_app.entity.Plan;
import com.haryokuncoro.subscription_app.entity.User;
import com.haryokuncoro.subscription_app.exception.StripeOperationException;
import com.haryokuncoro.subscription_app.repository.UserRepository;
import com.haryokuncoro.subscription_app.stripe.StripeKeyResolver;
import com.haryokuncoro.subscription_app.utils.GeneralUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StripeService {
    private final UserRepository userRepository;
    private final StripeKeyResolver stripeKeyResolver;

    public String createCustomer(User user) throws StripeException {
        String apiKey = stripeKeyResolver.resolveApiKey(user.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFullName())
                .build();

        try {
            Customer customer = Customer.create(params, options);
            return customer.getId();
        }catch (StripeException e){
            log.error("fail to create stripe customer", e);
            throw e;
        }
    }

    @Async
    protected void createStripeCustomerAsync(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            try {
                String customerId = this.createCustomer(user);
                user.setStripeCustomerId(customerId);
                userRepository.save(user);
            } catch (Exception e) {
                log.error("Failed to create Stripe customer for user {}", userId, e);
            }
        });
    }

    public StripePlanResult createProductAndPrice(PlanRequest request) {
        try {
            Product stripeProduct = createProduct(request);
            Price stripePrice = createPrice(request, stripeProduct.getId());
            return new StripePlanResult(stripeProduct.getId(), stripePrice.getId());
        } catch (StripeException e) {
            throw new StripeOperationException("Failed to create Stripe product/price", e);
        }
    }

    private Product createProduct(PlanRequest request) throws StripeException {
        String apiKey = stripeKeyResolver.resolveApiKey(request.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        ProductCreateParams params = ProductCreateParams.builder()
                .setName(request.getName())
                .setDescription(request.getDescription())
                .build();
        return Product.create(params, options);
    }

    private Price createPrice(PlanRequest request, String stripeProductId) throws StripeException {
        String apiKey = stripeKeyResolver.resolveApiKey(request.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        Long amount = GeneralUtils.toCents(request.getAmount());
        PriceCreateParams.Builder builder = PriceCreateParams.builder()
                .setProduct(stripeProductId)
                .setCurrency(request.getCurrency().toLowerCase())
                .setUnitAmount(amount);

        if (request.getBillingInterval() != null) {
            builder.setRecurring(
                    PriceCreateParams.Recurring.builder()
                            .setInterval(toStripeInterval(request.getBillingInterval()))
                            .build()
            );
        }

        return Price.create(builder.build(), options);
    }

    public StripePlanResult updateProductAndPrice(Plan existingPlan, PlanRequest request) {
        try {
            updateProduct(existingPlan.getStripeProductId(), request);

            boolean priceChanged = priceNeedsUpdate(existingPlan, request);
            String stripePriceId = existingPlan.getStripePriceId();

            if (priceChanged) {
                // archive old price, create a new one
                archivePrice(request, existingPlan.getStripePriceId());
                Price newPrice = createPrice(request, existingPlan.getStripeProductId());
                stripePriceId = newPrice.getId();
            }

            return new StripePlanResult(existingPlan.getStripeProductId(), stripePriceId);
        } catch (StripeException e) {
            throw new StripeOperationException("Failed to update Stripe product/price", e);
        }
    }

    private void updateProduct(String stripeProductId, PlanRequest request) throws StripeException {
        String apiKey = stripeKeyResolver.resolveApiKey(request.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        Product product = Product.retrieve(stripeProductId, options);
        ProductUpdateParams params = ProductUpdateParams.builder()
                .setName(request.getName())
                .setDescription(request.getDescription())
                .setActive(request.getActive())
                .build();
        product.update(params, options);
    }

    private boolean priceNeedsUpdate(Plan existingPlan, PlanRequest request) {
        long existingAmountCents = existingPlan.getAmount();
        long newAmountCents = GeneralUtils.toCents(request.getAmount());
        return existingAmountCents != newAmountCents
                || !existingPlan.getCurrency().equalsIgnoreCase(request.getCurrency())
                || !existingPlan.getBillingInterval().equals(request.getBillingInterval());
    }

    private void archivePrice(PlanRequest request, String stripePriceId) throws StripeException {
        String apiKey = stripeKeyResolver.resolveApiKey(request.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        Price price = Price.retrieve(stripePriceId);
        PriceUpdateParams params = PriceUpdateParams.builder()
                .setActive(false)
                .build();
        price.update(params, options);
    }

    public void archiveProductAndPrice(Plan plan) {
        String apiKey = stripeKeyResolver.resolveApiKey(plan.getCountry());
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        try {
            Price price = Price.retrieve(plan.getStripePriceId(), options);
            price.update(PriceUpdateParams.builder().setActive(false).build(), options);

            Product product = Product.retrieve(plan.getStripeProductId(), options);
            product.update(ProductUpdateParams.builder().setActive(false).build(), options);
        } catch (StripeException e) {
            throw new StripeOperationException("Failed to archive Stripe product/price", e);
        }
    }

    private PriceCreateParams.Recurring.Interval toStripeInterval(BillingInterval interval) {
        return switch (interval) {
            case MONTH -> PriceCreateParams.Recurring.Interval.MONTH;
            case YEAR -> PriceCreateParams.Recurring.Interval.YEAR;
            default -> throw new IllegalArgumentException("Unsupported billing interval: " + interval);
        };
    }

    public com.stripe.model.Subscription createSubscription(String stripeCustomerId, String country, String stripePriceId) {
        String apiKey = stripeKeyResolver.resolveApiKey(country);
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        try {
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .addItem(
                            SubscriptionCreateParams.Item.builder()
                                    .setPrice(stripePriceId)
                                    .build()
                    )
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .setCollectionMethod(SubscriptionCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                    .addExpand("latest_invoice.confirmation_secret")
                    .build();

            return com.stripe.model.Subscription.create(params, options);
        } catch (StripeException e) {
            throw new StripeOperationException("Failed to create Stripe subscription", e);
        }
    }

    public com.stripe.model.Subscription updateSubscription(String country, String stripeSubscriptionId, String newStripePriceId, Boolean cancelAtPeriodEnd) {
        String apiKey = stripeKeyResolver.resolveApiKey(country);
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        try {
            com.stripe.model.Subscription stripeSubscription =
                    com.stripe.model.Subscription.retrieve(stripeSubscriptionId, options);

            SubscriptionUpdateParams.Builder builder = SubscriptionUpdateParams.builder();

            if (newStripePriceId != null) {
                String itemId = stripeSubscription.getItems().getData().get(0).getId();
                builder.addItem(
                        SubscriptionUpdateParams.Item.builder()
                                .setId(itemId)
                                .setPrice(newStripePriceId)
                                .build()
                );
                builder.setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS);
            }

            if (cancelAtPeriodEnd != null) {
                builder.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            }

            return stripeSubscription.update(builder.build(), options);
        } catch (StripeException e) {
            throw new StripeOperationException("Failed to update Stripe subscription", e);
        }
    }

    public com.stripe.model.Subscription cancelSubscription(String country, String stripeSubscriptionId, boolean immediately) {
        String apiKey = stripeKeyResolver.resolveApiKey(country);
        RequestOptions options = RequestOptions.builder().setApiKey(apiKey).build();
        try {
            com.stripe.model.Subscription stripeSubscription =
                    com.stripe.model.Subscription.retrieve(stripeSubscriptionId, options);

            if (immediately) {
                SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                                .build();
                return stripeSubscription.cancel(params, options);
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                return stripeSubscription.update(params, options);
            }
        } catch (StripeException e) {
            throw new StripeOperationException("Failed to cancel Stripe subscription", e);
        }
    }
}