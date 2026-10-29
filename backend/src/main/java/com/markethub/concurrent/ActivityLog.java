package com.markethub.concurrent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Recent-activity feed shown on the admin dashboard.
 *
 * <p>Many request threads and background workers write to it at once, so it is built on
 * {@link ConcurrentLinkedDeque}, a lock-free queue. New events go to the front; the oldest are
 * dropped once {@value #MAX_EVENTS} are held. Only the most recent events are kept in memory;
 * this is a monitoring aid, not an audit trail.</p>
 */
public class ActivityLog {

    private static final int MAX_EVENTS = 200;

    /** One immutable log entry. */
    public static final class Event {
        private final Instant at;
        private final String actor;
        private final String message;

        Event(Instant at, String actor, String message) {
            this.at = at;
            this.actor = actor;
            this.message = message;
        }

        public Instant getAt() { return at; }
        public String getActor() { return actor; }
        public String getMessage() { return message; }

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("at", at.toString());
            view.put("actor", actor);
            view.put("message", message);
            return view;
        }
    }

    private final ConcurrentLinkedDeque<Event> events = new ConcurrentLinkedDeque<>();

    public void record(String actor, String message) {
        events.addFirst(new Event(Instant.now(), actor, message));
        while (events.size() > MAX_EVENTS) {
            events.pollLast();
        }
    }

    /** @return up to {@code limit} events, newest first */
    public List<Event> recent(int limit) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (result.size() >= limit) {
                break;
            }
            result.add(event);
        }
        return result;
    }
}
