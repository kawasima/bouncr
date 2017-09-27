package net.unit8.bouncr;

import net.unit8.bouncr.util.PasswordUtils;
import org.junit.jupiter.api.Test;

import static org.quicktheories.quicktheories.QuickTheory.qt;
import static org.quicktheories.quicktheories.generators.SourceDSL.strings;

public class QuickTheoriesTest {
    @Test
    public void addingTwoPositiveIntegersAlwaysGivesAPositiveInteger(){
        qt()
                .forAll(strings().ascii().ofLengthBetween(8, 255)
                        , strings().ascii().ofLength(16))
                .check((password, salt) -> {
                    byte[] hash = PasswordUtils.pbkdf2("password", "salt", 100);
                    return hash != null && hash.length > 0;
                });
    }
}
