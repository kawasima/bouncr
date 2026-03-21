package net.unit8.bouncr.data;

import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Certificate metadata associated with a user.
 *
 * @param id persistent identifier
 * @param user owner of the certificate
 * @param serial certificate serial number
 * @param expires expiration date
 */
public record Cert(Long id, User user, BigInteger serial, LocalDate expires) {
}
