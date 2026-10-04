package com.pulsepass.dto.request;

import com.pulsepass.domain.enums.TicketType;

public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type
) {}