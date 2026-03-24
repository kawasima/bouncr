package net.unit8.bouncr.api.boundary;

/**
 * Request body for updating an application.
 *
 * @param name application name
 * @param description human-readable description of the application
 * @param virtualPath URL path prefix that the application is mounted on
 * @param passTo upstream URL to proxy requests to
 * @param topPage default landing page path
 */
public record ApplicationUpdate(String name, String description, String virtualPath, String passTo, String topPage) {}
