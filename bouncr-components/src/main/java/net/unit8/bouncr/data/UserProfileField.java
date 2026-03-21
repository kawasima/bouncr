package net.unit8.bouncr.data;

/**
 * User profile field definition.
 *
 * @param id persistent identifier
 * @param name internal field name
 * @param jsonName API/JSON field name
 * @param isRequired whether the field is mandatory
 * @param isIdentity whether the field can be used as identity information
 * @param regularExpression optional validation regex
 * @param maxLength maximum length constraint
 * @param minLength minimum length constraint
 * @param needsVerification whether values must be verified
 * @param position display/sort position
 */
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
