package me.xap3y.space.api.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Supplier;

public enum StatTimePreset {
    TODAY(
            () -> LocalDateTime.now().with(LocalTime.MIN),
            LocalDateTime::now
    ),
    YESTERDAY(
            () -> LocalDateTime.now().minusDays(1).with(LocalTime.MIN),
            () -> LocalDateTime.now().minusDays(1).with(LocalTime.MAX)
    ),
    THIS_WEEK(
            () -> LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(),
            LocalDateTime::now
    ),
    THIS_YEAR(
            () -> LocalDate.of(LocalDate.now().getYear(), 1, 1).atStartOfDay(),
            LocalDateTime::now
    ),
    LAST_60_MINUTES(
            () -> LocalDateTime.now().minusMinutes(60),
            LocalDateTime::now
    ),
    LAST_24_HOURS(
            () -> LocalDateTime.now().minusHours(24),
            LocalDateTime::now
    ),
    LAST_7_DAYS(
            () -> LocalDateTime.now().minusDays(7),
            LocalDateTime::now
    ),
    LAST_14_DAYS(
            () -> LocalDateTime.now().minusDays(14),
            LocalDateTime::now
    ),
    LAST_30_DAYS(
            () -> LocalDateTime.now().minusDays(30),
            LocalDateTime::now
    ),
    LAST_90_DAYS(
            () -> LocalDateTime.now().minusDays(90),
            LocalDateTime::now
    ),
    TOTAL(
            () -> LocalDateTime.of(2024, 1, 1, 0, 0),
            LocalDateTime::now
    );

    private final Supplier<LocalDateTime> fromSupplier;
    private final Supplier<LocalDateTime> toSupplier;

    StatTimePreset(Supplier<LocalDateTime> fromSupplier, Supplier<LocalDateTime> toSupplier) {
        this.fromSupplier = fromSupplier;
        this.toSupplier = toSupplier;
    }

    public LocalDateTime getFrom() {
        return fromSupplier.get();
    }

    public LocalDateTime getTo() {
        return toSupplier.get();
    }
}
