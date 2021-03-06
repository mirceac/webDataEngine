package security.util;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import play.Logger;
import security.token.ClientToken;
import security.token.ClientType;

public class TokenUtil {

    public static String getUsernameFromToken(Authentication token) {
        if (token instanceof ClientAuthenticationToken) {
            ClientAuthenticationToken clientToken = (ClientAuthenticationToken)token;
            if(StringUtils.equals(clientToken.getClientName(), ClientType.OidcClient.name())) {
                OidcCredentials oidcCredentials = (OidcCredentials)clientToken.getCredentials();
                return oidcCredentials.getCode().getValue();
            }
        } else if (token instanceof ClientToken) {
            ClientToken clientToken = (ClientToken) token;
            if (clientToken.getClientType() == ClientType.FormClient ||
                    clientToken.getClientType() == ClientType.SessionClient ||
                    clientToken.getClientType() == ClientType.DirectBasicAuthClient || clientToken.getClientType() == ClientType.DirectDigestAuthClient) {
                Object principal = token.getPrincipal();
                if (principal != null) {
                    return principal.toString();
                }
            }
        }
        return null;
    }

    public static String getAccountIdFromToken(Authentication token) {
        if (token instanceof ClientAuthenticationToken) {
            ClientAuthenticationToken clientToken = (ClientAuthenticationToken)token;
            if(StringUtils.equals(clientToken.getClientName(), ClientType.OidcClient.name())) {
                return (String)clientToken.getUserProfile().getAttribute("email");
            }
        } else if (token instanceof ClientToken) {
            ClientToken clientToken = (ClientToken) token;
            return (String)clientToken.getUserProfile().getAttribute("accountid");
        }
        return null;
    }

    public static String getCurrentAccountId() {
        String accountId = TokenUtil.getAccountIdFromToken(SecurityContextHolder.getContext().getAuthentication());
        Logger.info("CurrentTenant: " + accountId);
        return accountId;
    }
}
