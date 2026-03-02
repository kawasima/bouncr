package net.unit8.bouncr.api.authn;

import enkan.data.HttpRequest;
import enkan.security.AuthBackend;
import enkan.security.bouncr.UserPermissionPrincipal;
import jakarta.inject.Inject;
import net.unit8.bouncr.component.StoreProvider;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static net.unit8.bouncr.component.StoreProvider.StoreType.BOUNCR_TOKEN;

public class TokenAuthBackend implements AuthBackend<HttpRequest, Map<String, Object>> {
    @Inject
    private StoreProvider storeProvider;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(HttpRequest request) {
        String token = (String) request.getHeaders().get("x-bouncr-token");
        if (token == null) return null;
        Object stored = storeProvider.getStore(BOUNCR_TOKEN).read(token);
        if (!(stored instanceof Map)) return null;
        return new HashMap<>((Map<String, Object>) stored);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Principal authenticate(HttpRequest request, Map<String, Object> authenticationData) {
        if (authenticationData == null) return null;
        Long id = Long.valueOf(Objects.toString(authenticationData.remove("uid"), "0"));
        String account = (String) authenticationData.remove("sub");
        Map<String, List<String>> permissionsByRealm = Optional.ofNullable(authenticationData.remove("permissionsByRealm"))
                .filter(Map.class::isInstance)
                .map(m -> (Map<String, List<String>>) m)
                .orElse(Collections.emptyMap());
        Set<String> permissions = permissionsByRealm.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return new UserPermissionPrincipal(id, account, authenticationData, permissions);
    }
}
