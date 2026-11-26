package contracts.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formats the contract timestamps stored in DynamoDB as strings.
 *
 * <p>The form is {@code yyyy-MM-ddTHH:mm:ssZ}: UTC, whole seconds (any fraction is truncated), literal {@code Z}.
 * Every value has the same width, so comparing the strings orders them by time.
 * {@link Instant#toString()} can't be used because it prints a varying number of fraction digits.
 */
public final class ContractTimestamp {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private ContractTimestamp() {
    }

    /** Formats an instant as a contract timestamp. */
    public static String format(Instant instant) {
        return FORMAT.format(instant);
    }

    /** The current instant as a contract timestamp. */
    public static String now() {
        return format(Instant.now());
    }
}
