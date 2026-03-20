package net.unit8.bouncr.data;

import java.time.LocalDateTime;

public record PasswordCredential(
     User user,
     byte[] password,
     String salt,
     boolean initial,
     LocalDateTime createdAt) {
}
