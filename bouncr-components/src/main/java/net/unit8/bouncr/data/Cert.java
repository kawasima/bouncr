package net.unit8.bouncr.data;

import java.math.BigInteger;
import java.time.LocalDate;

public record Cert(Long id, User user, BigInteger serial, LocalDate expires) {
}
