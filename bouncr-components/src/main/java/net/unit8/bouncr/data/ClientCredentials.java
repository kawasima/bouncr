package net.unit8.bouncr.data;

/**
 * OAuth2 client credentials — {@code client_id} and {@code client_secret}
 * as an inseparable pair.
 *
 * <p>In OAuth2, client authentication always requires both values together.
 * They may arrive via HTTP Basic auth ({@code Authorization: Basic base64(id:secret)})
 * or via POST body parameters ({@code client_id} + {@code client_secret}).
 *
 * <p>Parsing from HTTP headers or form parameters is the responsibility of
 * the decoder layer (e.g., {@code BouncrFormDecoders}), not this record.
 * This record is a pure data type representing the validated pair.
 *
 * @param clientId     the OAuth2 client identifier
 * @param clientSecret the client secret (plaintext, for verification against stored hash)
 */
public record ClientCredentials(String clientId, String clientSecret) {}
