package net.unit8.bouncr.data;

public record UserProfileField(
    Long id,
    String name,
    String jsonName,
    boolean isRequired,
    boolean isIdentity,
    String regularExpression,
    Integer maxLength,
    Integer minLength,
    boolean needsVerification,
    Integer position
) {}
