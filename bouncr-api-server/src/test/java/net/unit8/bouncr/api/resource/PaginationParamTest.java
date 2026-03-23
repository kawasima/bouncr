package net.unit8.bouncr.api.resource;

import net.unit8.bouncr.api.util.PaginationParams;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PaginationParams} — safe parsing of pagination query parameters.
 */
class PaginationParamTest {

    @Nested
    class ParseOffset {
        @Test
        void null_returnsZero() {
            assertThat(PaginationParams.parseOffset(null)).isEqualTo(0);
        }

        @Test
        void validNumber_returnsParsed() {
            assertThat(PaginationParams.parseOffset("20")).isEqualTo(20);
        }

        @Test
        void zero_returnsZero() {
            assertThat(PaginationParams.parseOffset("0")).isEqualTo(0);
        }

        @Test
        void negative_clampedToZero() {
            assertThat(PaginationParams.parseOffset("-5")).isEqualTo(0);
        }

        @Test
        void nonNumeric_returnsZero() {
            assertThat(PaginationParams.parseOffset("abc")).isEqualTo(0);
        }

        @Test
        void empty_returnsZero() {
            assertThat(PaginationParams.parseOffset("")).isEqualTo(0);
        }

        @Test
        void veryLargeNumber_returnsParsed() {
            assertThat(PaginationParams.parseOffset("999999")).isEqualTo(999999);
        }

        @Test
        void overflow_returnsZero() {
            assertThat(PaginationParams.parseOffset("99999999999999")).isEqualTo(0);
        }
    }

    @Nested
    class ParseLimit {
        @Test
        void null_returnsDefault() {
            assertThat(PaginationParams.parseLimit(null, 10)).isEqualTo(10);
        }

        @Test
        void validNumber_returnsParsed() {
            assertThat(PaginationParams.parseLimit("25", 10)).isEqualTo(25);
        }

        @Test
        void one_returnsParsed() {
            assertThat(PaginationParams.parseLimit("1", 10)).isEqualTo(1);
        }

        @Test
        void thousand_returnsParsed() {
            assertThat(PaginationParams.parseLimit("1000", 10)).isEqualTo(1000);
        }

        @Test
        void zero_returnsDefault() {
            assertThat(PaginationParams.parseLimit("0", 10)).isEqualTo(10);
        }

        @Test
        void negative_returnsDefault() {
            assertThat(PaginationParams.parseLimit("-1", 10)).isEqualTo(10);
        }

        @Test
        void exceedsMax_returnsDefault() {
            assertThat(PaginationParams.parseLimit("1001", 10)).isEqualTo(10);
        }

        @Test
        void nonNumeric_returnsDefault() {
            assertThat(PaginationParams.parseLimit("xyz", 10)).isEqualTo(10);
        }

        @Test
        void empty_returnsDefault() {
            assertThat(PaginationParams.parseLimit("", 10)).isEqualTo(10);
        }

        @Test
        void overflow_returnsDefault() {
            assertThat(PaginationParams.parseLimit("99999999999999", 10)).isEqualTo(10);
        }
    }

    @Nested
    class ParseLong {
        @Test
        void null_returnsNull() {
            assertThat(PaginationParams.parseLong(null)).isNull();
        }

        @Test
        void validNumber_returnsParsed() {
            assertThat(PaginationParams.parseLong("42")).isEqualTo(42L);
        }

        @Test
        void negative_returnsParsed() {
            assertThat(PaginationParams.parseLong("-1")).isEqualTo(-1L);
        }

        @Test
        void nonNumeric_returnsNull() {
            assertThat(PaginationParams.parseLong("abc")).isNull();
        }

        @Test
        void empty_returnsNull() {
            assertThat(PaginationParams.parseLong("")).isNull();
        }

        @Test
        void largeValue_returnsParsed() {
            assertThat(PaginationParams.parseLong("9223372036854775807")).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        void overflow_returnsNull() {
            assertThat(PaginationParams.parseLong("99999999999999999999")).isNull();
        }
    }
}
