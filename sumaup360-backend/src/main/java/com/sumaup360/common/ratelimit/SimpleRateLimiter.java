package com.sumaup360.common.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limitador de tasa simple en memoria (ventana fija por clave: minuto o dia).
 * Suficiente para proteger endpoints publicos de abuso/enumeracion en un despliegue de una
 * sola instancia. Para multi-instancia migrar a Redis/bucket distribuido.
 *
 * Las ventanas vencidas se purgan periodicamente para que el mapa no crezca sin limite
 * (una clave por IP/token; sin purga, un atacante podria inflar la memoria del proceso).
 */
@Component
public class SimpleRateLimiter {

    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 86_400_000L;
    private static final long CLEANUP_EVERY_MS = 5 * MINUTE_MS;
    private static final int CLEANUP_IF_KEYS_OVER = 100_000;

    private static final class Window {
        final long span;
        long start;
        int count;

        Window(long span, long now) {
            this.span = span;
            this.start = now;
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    /** Devuelve true si la accion esta permitida; false si supera maxPerMinute para esa clave. */
    public boolean allow(String key, int maxPerMinute) {
        return allow(key, maxPerMinute, MINUTE_MS);
    }

    /** Igual que allow, pero con ventana de 24 horas (topes diarios por token). */
    public boolean allowDaily(String key, int maxPerDay) {
        return allow("day:" + key, maxPerDay, DAY_MS);
    }

    private boolean allow(String key, int max, long span) {
        long now = System.currentTimeMillis();
        maybeCleanup(now);
        Window w = windows.computeIfAbsent(key, k -> new Window(span, now));
        synchronized (w) {
            if (now - w.start > w.span) {
                w.start = now;
                w.count = 0;
            }
            w.count++;
            return w.count <= max;
        }
    }

    private void maybeCleanup(long now) {
        long last = lastCleanup.get();
        boolean due = now - last > CLEANUP_EVERY_MS || windows.size() > CLEANUP_IF_KEYS_OVER;
        if (due && lastCleanup.compareAndSet(last, now)) {
            windows.entrySet().removeIf(e -> now - e.getValue().start > e.getValue().span);
        }
    }
}
