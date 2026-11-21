package contracts;

import contracts.utils.ContractTimestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContractTimestampTests {

    @Test
    public void formatTruncatesTheFractionInsteadOfRounding() {
        Instant instant = Instant.parse("2026-11-21T02:42:30.987654321Z");

        assertEquals("2026-11-21T02:42:30Z", ContractTimestamp.format(instant));
    }

    @Test
    public void formatKeepsTheSameWidthWhenTheFractionIsZero() {
        // Instant.toString() would print "2026-11-21T02:42:30Z" here but "...30.5Z" for a half second
        Instant instant = Instant.parse("2026-11-21T02:42:30Z");

        assertEquals("2026-11-21T02:42:30Z", ContractTimestamp.format(instant));
        assertEquals(20, ContractTimestamp.format(instant).length());
    }

    @Test
    public void formatIsUtcRegardlessOfTheDefaultTimeZone() {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Australia/Sydney"));

            assertEquals("2026-11-21T02:42:30Z", ContractTimestamp.format(Instant.parse("2026-11-21T02:42:30.987Z")));
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    @Test
    public void nowMatchesTheContractTimestampPattern() {
        assertTrue(ContractTimestamp.now().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$"));
    }
}
