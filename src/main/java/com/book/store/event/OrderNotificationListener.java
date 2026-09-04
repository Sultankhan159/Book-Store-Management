package com.book.store.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    @Async
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("== [ASYNC EVENT] Sending confirmation email and dispatching notification for Order ID: #{} (Total: ${}) to User: {} ==",
                event.getOrder().getId(),
                event.getOrder().getTotalAmount(),
                event.getOrder().getUser() != null ? event.getOrder().getUser().getUsername() : "Guest");

        try {
            // Simulate 500ms network IO for sending email or publishing to external webhook
            Thread.sleep(500);
            log.info("== [ASYNC EVENT] Confirmation email successfully delivered for Order ID: #{} ==", event.getOrder().getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Async email dispatch interrupted for order: {}", event.getOrder().getId(), e);
        }
    }
}
