package com.logai.deploy.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorSummaryTests {

    @Test
    void constructorStoresInitialValues() {

        ErrorSummary summary =
                new ErrorSummary(
                        "Connection refused",
                        "Connection refused",
                        "2026-08-11 10:00:00"
                );

        assertEquals(1, summary.getCount());
        assertEquals(
                "Connection refused",
                summary.getSampleLine()
        );

        assertEquals(
                "2026-08-11 10:00:00",
                summary.getFirstOccurrence()
        );

        assertEquals(
                "2026-08-11 10:00:00",
                summary.getLastOccurrence()
        );
    }

    @Test
    void incrementUpdatesCountAndLastOccurrence() {

        ErrorSummary summary =
                new ErrorSummary(
                        "Connection refused",
                        "Connection refused",
                        "2026-08-11 10:00:00"
                );

        summary.addOccurrence("2026-08-11 10:05:00");

        assertEquals(2, summary.getCount());

        assertEquals(
                "2026-08-11 10:05:00",
                summary.getLastOccurrence()
        );

        assertEquals(
                "2026-08-11 10:00:00",
                summary.getFirstOccurrence()
        );
    }
}