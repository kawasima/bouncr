package net.unit8.bouncr.data;

/**
 * @param name display name
 * @param description optional description
 * @param passTo upstream target URL
 * @param virtualPath external path prefix
 * @param topPage default landing page
 */
public record ApplicationSpec(WordName name, String description, String passTo, String virtualPath, String topPage) {
}
