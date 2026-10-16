package com.pulsepass.dto.response;

import com.pulsepass.domain.enums.EventStatus;
import java.time.LocalDateTime;

public record EventSummaryResponse(
        String eventCode,
        String name,
        EventStatus status,
        LocalDateTime eventDate,
        String venueName
) {}