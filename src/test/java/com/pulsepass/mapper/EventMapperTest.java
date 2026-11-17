package com.pulsepass.mapper;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventMapperTest {

    @ParameterizedTest
    @EnumSource(EventCategory.class)
    void summaryPreservesCategoryAndExistingFields(EventCategory category) {
        Venue venue = new Venue();
        venue.setName("Test venue");
        Event event = new Event();
        event.setEventCode("EVT-CATEGORY");
        event.setName("Category event");
        event.setCategory(category);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.of(2028, 6, 15, 20, 0));
        event.setVenue(venue);

        var summary = new EventMapperImpl().toSummary(event);

        assertEquals(category, summary.category());
        assertEquals(event.getEventCode(), summary.eventCode());
        assertEquals(event.getName(), summary.name());
        assertEquals(event.getStatus(), summary.status());
        assertEquals(event.getEventDate(), summary.eventDate());
        assertEquals(venue.getName(), summary.venueName());
    }
}
