package couchdb.config;

import couchdb.dao.CouchDBRoleRepository;
import couchdb.dao.CouchDBUserRepository;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
@ComponentScan("couchdb.service")
public class CouchdbConfig {

    @Autowired
    ConfigurableEnvironment env;

    @Bean
    public HttpClient couchDBHttpClient() {
        HttpClient httpClient = null;
        try {
            httpClient = new StdHttpClient.Builder()
                    .url(env.getProperty("uri"))
                    .build();
        } catch (Exception e) {
            return null;
        }
        return httpClient;
    }

    @Bean
    public CouchDbConnector couchdbUserConnector() {
        return couchdbConnector("users");
    }

    @Bean
    public CouchDbConnector couchdbRoleConnector() {
        return couchdbConnector("roles");
    }

    private CouchDbConnector couchdbConnector(String databaseName) {
        CouchDbConnector db = null;
        try {
            CouchDbInstance dbInstance = new StdCouchDbInstance(couchDBHttpClient());
            db = new StdCouchDbConnector(databaseName, dbInstance);

            db.createDatabaseIfNotExists();
        } catch (Exception e) {
            return null;
        }
        return db;
    }

    @Bean
    public CouchDBUserRepository userRepository() {
        return new CouchDBUserRepository(couchdbUserConnector());
    }

    @Bean
    public CouchDBRoleRepository roleRepository() {
        return new CouchDBRoleRepository(couchdbRoleConnector());
    }
}
