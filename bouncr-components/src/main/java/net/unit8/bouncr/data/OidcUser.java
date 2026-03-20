package net.unit8.bouncr.data;

public record OidcUser(
    OidcProvider oidcProvider,
    User user,
    String oidcSub
) {}