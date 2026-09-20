package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

final class TestData {

    private TestData() {
    }

    static Venue venue(String code, String city) {
        Venue v = new Venue();
        v.setCode(code);
        v.setName("Venue " + code);
        v.setCity(city);
        v.setAddress("Calle 1 # 2-3");
        v.setCapacity(5000);
        v.setActive(true);
        return v;
    }

    static Event event(String code, Venue venue, EventStatus status, LocalDateTime date) {
        Event e = new Event();
        e.setEventCode(code);
        e.setName("Evento " + code);
        e.setCategory(EventCategory.MUSIC);
        e.setStatus(status);
        e.setEventDate(date);
        e.setMinimumAge(0);
        e.setVenue(venue);
        return e;
    }

    static User user(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setActive(true);
        return u;
    }

    static Ticket ticket(String code, User user, Event event,
                         TicketType type, TicketStatus status, String price) {
        Ticket t = new Ticket();
        t.setTicketCode(code);
        t.setUser(user);
        t.setEvent(event);
        t.setType(type);
        t.setStatus(status);
        t.setPrice(new BigDecimal(price));
        t.setPurchaseDate(LocalDateTime.now());
        return t;
    }
}
