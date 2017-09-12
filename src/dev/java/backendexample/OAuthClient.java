package backendexample;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import enkan.Env;

import java.util.concurrent.Future;

public class OAuthClient {
    public static void main(String[] args) {
        OAuth20Service service = new ServiceBuilder(Env.get("CLIENT_ID"))
                .apiSecret(Env.get("CLIENT_SECRET"))
                .build(new DefaultApi20() {

                    @Override
                    public String getAccessTokenEndpoint() {
                        return "http://localhost:3000/my/oauth/accessToken";
                    }

                    @Override
                    protected String getAuthorizationBaseUrl() {
                        return "http://localhost:3000/my/oauth/authorize";
                    }

                    @Override
                    public Verb getAccessTokenVerb() {
                        return Verb.GET;
                    }
                });

        Future<OAuth2AccessToken> accessTokenFuture = service.getAccessToken(Env.get("CODE"), new OAuthAsyncRequestCallback<OAuth2AccessToken>() {
            @Override
            public void onCompleted(OAuth2AccessToken oAuth2AccessToken) {

            }

            @Override
            public void onThrowable(Throwable throwable) {

            }
        });

    }
}
