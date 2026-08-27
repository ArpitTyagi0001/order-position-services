package com.order_update_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class OrderEvent {

    @NotBlank
    private String eventId;

    @NotBlank
    private String symbol;

    @Pattern(regexp = "BUY|SELL")
    private String transactionType;

    @Positive
    private Long quantity;

    public OrderEvent(String eventId, String symbol, String transactionType, Long quantity) {
        this.eventId = eventId;
        this.symbol = symbol;
        this.transactionType = transactionType;
        this.quantity = quantity;
    }

    public OrderEvent() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
