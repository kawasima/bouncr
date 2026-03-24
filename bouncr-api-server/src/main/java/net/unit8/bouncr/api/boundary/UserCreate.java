package net.unit8.bouncr.api.boundary;

/**
 * Request body for admin-initiated user creation.
 *
 * @param account user account name
 */
public record UserCreate(String account) {}
