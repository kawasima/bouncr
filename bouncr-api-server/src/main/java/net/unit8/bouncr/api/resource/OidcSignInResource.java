package net.unit8.bouncr.api.resource;

import enkan.collection.Multimap;
import enkan.collection.Parameters;
import enkan.data.Cookie;
import enkan.data.HttpRequest;
import enkan.util.CodecUtils;
import enkan.util.ThreadingUtils;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.data.OidcSession;
import net.unit8.bouncr.entity.OidcProvider;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import java.io.Serializable;
import java.util.Objects;

import static enkan.util.ThreadingUtils.some;
import static kotowari.restful.DecisionPoint.EXISTS;
import static kotowari.restful.DecisionPoint.HANDLE_OK;
import static kotowari.restful.DecisionPoint.PROCESSABLE;
import static net.unit8.bouncr.component.StoreProvider.StoreType.OIDC_SESSION;
import static net.unit8.bouncr.entity.ResponseType.ID_TOKEN;
import static net.unit8.bouncr.entity.ResponseType.TOKEN;

@AllowedMethods({"GET"})
public class OidcSignInResource {
    @Inject
    private StoreProvider storeProvider;

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        OidcProvider oidcProvider = em.find(OidcProvider.class, params.getLong("id"));
        if (oidcProvider != null) {
            context.putValue(oidcProvider);
        }
        return oidcProvider != null;
    }

    @Decision(PROCESSABLE)
    public boolean processable(Parameters params, OidcSession oidcSession) {
        if (!Objects.equals(oidcSession.getState(), params.get("state"))) {
            return false;
        }
        return true;
    }

    @Decision(HANDLE_OK)
    public Void callback(HttpRequest request, OidcProvider oidcProvider) {
        String oidcSessionId = some(request.getCookies().get("OIDC_SESSION_ID"), Cookie::getValue).orElse(null);
        OidcSession oidcSession = (OidcSession) storeProvider.getStore(OIDC_SESSION).read(oidcSessionId);
        if (oidcProvider.getResponseType() == ID_TOKEN && oidcProvider.getResponseType() == TOKEN) {
            // TODO
        }
        return null;
    }

    public static class OidcProviderDto implements Serializable {
        private Long id;
        private String name;
        private String apiKey;
        private String scope;
        private String state = RandomUtils.generateRandomString(8);
        private String responseType;
        private String tokenEndpoint;
        private String authorizationEndpoint;
        private String nonce = RandomUtils.generateRandomString(32);
        private String redirectUriBase;

        public String getRedirectUri() {
            return redirectUriBase
                    + "/my/signIn/oidc/" + id;
        }

        public String getAuthorizationUrl() {
            return authorizationEndpoint + "?response_type=" + CodecUtils.urlEncode(responseType)
                    + "&client_id=" + apiKey
                    + "&redirect_uri=" + CodecUtils.urlEncode(getRedirectUri())
                    + "&state=" + state
                    + "&scope=" + scope
                    + "&nonce=" + nonce;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getResponseType() {
            return responseType;
        }

        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public void setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
        }

        public String getAuthorizationEndpoint() {
            return authorizationEndpoint;
        }

        public void setAuthorizationEndpoint(String authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public String getRedirectUriBase() {
            return redirectUriBase;
        }

        public void setRedirectUriBase(String redirectUriBase) {
            this.redirectUriBase = redirectUriBase;
        }
    }
}
