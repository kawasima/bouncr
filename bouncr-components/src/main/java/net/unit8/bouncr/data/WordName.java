package net.unit8.bouncr.data;

/**
 * A validated name consisting of word characters ({@code \w+}), max 100 characters.
 *
 * <p>Used for account names, group names, role names, realm names,
 * application names, and OIDC provider/application names.
 *
 * @param value the validated name string
 */
public record WordName(String value) {}
