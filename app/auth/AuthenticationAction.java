package auth;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.play.CallbackController;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import play.Logger;
import play.cache.CacheApi;
import play.cache.NamedCache;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;

public class AuthenticationAction extends Action.Simple {

    @Inject
    @NamedCache("session-cache")
    protected CacheApi sessionCache;


    protected void createAuthenticationTokenInSession(Http.Context context, String userName, String password) {
        sessionCache.set(userName, createToken(userName, password));
        context.session().clear();
        context.session().put("username", userName);
    }

    //protected void createOidcSocialAuthenticationTokenInSession(Http.Context context, OidcCredentials credentials, String clientName) {

    //    sessionCache.set(credentials.getCode().getValue(), createOidcToken(credentials, clientName));
    //    context.session().clear();
    //    context.session().put("username", credentials.getCode().getValue());
    //}

    protected void createRequestAuthenticationToken(String userName, String password) {
        SecurityContextHolder.getContext().setAuthentication(createToken(userName, password));
    }

    private void clearRequestAuthenticationToken() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private UsernamePasswordAuthenticationToken createToken(String userName, String password) {
        return new UsernamePasswordAuthenticationToken(userName, password);
    }

    private ClientAuthenticationToken createOidcToken(OidcCredentials credentials, String clientName) {
        return new ClientAuthenticationToken(credentials, clientName);
    }

    @Override
    public F.Promise<Result> call(Http.Context context) throws Throwable {
        Logger.info("Filtering request via authentication Layer");
        clearRequestAuthenticationToken();
        return delegate.call(context);
    }

}
