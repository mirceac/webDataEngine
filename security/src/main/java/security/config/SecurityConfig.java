package security.config;

import api.config.ApiConfig;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.DirectDigestAuthClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.play.http.DefaultHttpActionAdapter;
import org.pac4j.play.store.PlayCacheStore;
import org.pac4j.springframework.security.authentication.ClientAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import security.provider.DigestDaoAuthenticationProvider;
import security.provider.SecurityDaoAuthenticationProvider;
import security.provider.SecuritySessionAuthenticationProvider;
import security.service.SecuritySocialUserDetailsService;
import security.validator.SecurityDigestAuthenticator;
import security.validator.SecurityUsernamePasswordAuthenticator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@ComponentScan("security.*")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Inject
    SecurityDaoAuthenticationProvider daoProvider;

    @Inject
    DigestDaoAuthenticationProvider digestDaoProvider;

    @Inject
    private SecuritySocialUserDetailsService userDetailsService;

    @Inject
    private SecuritySessionAuthenticationProvider sessionProvider;

    @Bean
    public ClientAuthenticationProvider clientAuthenticationProvider() {
        ClientAuthenticationProvider provider = new ClientAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setClients(authClients());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        List<AuthenticationProvider> authenticationProviderList = new ArrayList<AuthenticationProvider>();
        authenticationProviderList.add(sessionProvider);
        authenticationProviderList.add(daoProvider);
        authenticationProviderList.add(digestDaoProvider);
        authenticationProviderList.add(clientAuthenticationProvider());
        ProviderManager providerManager = new ProviderManager(authenticationProviderList);
        return providerManager;
    }

    @Bean
    public Client oidcClient() {
        final OidcClient oidcClient = new OidcClient();
        oidcClient.setClientID(ApiConfig.configuration.getString("oidc.clientId"));
        oidcClient.setSecret(ApiConfig.configuration.getString("oidc.secret"));
        oidcClient.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
        oidcClient.addCustomParam("prompt", "consent");

        return oidcClient;
    }

    @Bean
    public FormClient formClient() {
        FormClient formClient = new FormClient("/form", new SecurityUsernamePasswordAuthenticator());
        formClient.setCallbackUrl("/login");
        return formClient;
    }

    @Bean
    public DirectBasicAuthClient basicClient() {
        DirectBasicAuthClient basicAuthClient = new DirectBasicAuthClient(new SecurityUsernamePasswordAuthenticator());
        return basicAuthClient;
    }

    @Bean
    public DirectDigestAuthClient digestClient() {
        DirectDigestAuthClient digestAuthClient = new DirectDigestAuthClient(new SecurityDigestAuthenticator());
        digestAuthClient.setRealm("testRealm");
        return digestAuthClient;
    }

    @Bean
    public Clients authClients() {
        String baseUrl = ApiConfig.configuration.getString("baseUrl");
        Clients clients = new Clients(baseUrl + "/callback", oidcClient(), formClient(), basicClient());
        return clients;
    }

    @Bean
    public Config config() {
        Config config = new Config(authClients());
        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer<>("ROLE_ADMIN"));
        config.setHttpActionAdapter(new DefaultHttpActionAdapter());
        config.addAuthorizer("custom", new Authorizer() {
            @Override
            public boolean isAuthorized(WebContext webContext, List list) throws RequiresHttpAction {
                return true;
            }
        });
        return config;
    }

    @Bean
    public PlayCacheStore cacheStore() {
        return new PlayCacheStore();
    }

    @Bean
    public DefaultHttpActionAdapter httpActionAdapter() {
        return new DefaultHttpActionAdapter();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.parentAuthenticationManager(authenticationManager());
    }
}
