package io.fleetpulse.telemetry.rules;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertCooldown {

    private final Map<String, Instant> lastFired = new ConcurrentHashMap<>();

    /** @return true if enough time has passed since the last alert for this key. */
    public boolean tryAcquire(String key, Duration cooldown) {
        Instant now = Instant.now();
        boolean[] allowed = {false};
        lastFired.compute(key, (k, prev) -> {
            if (prev == null || prev.plus(cooldown).isBefore(now)) {
                allowed[0] = true;
                return now;
            }
            return prev;
        });
        return allowed[0];
    }
}