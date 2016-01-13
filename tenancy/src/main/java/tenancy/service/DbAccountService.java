package tenancy.service;

import api.config.ApiConfig;
import api.domain.Account;
import api.domain.DbType;
import api.service.AccountService;
import api.service.MultiRoleService;
import api.service.MultiUserService;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import play.Logger;
import tenancy.dao.DbAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;

@Component
public class DbAccountService implements AccountService {

    @Autowired
    DbAccountRepository accountRepository;

    @Autowired
    DBCollection accountCollection;

    @Inject
    MultiUserService userService;

    @Inject
    MultiRoleService roleService;

    @Inject
    private PasswordEncoder passwordEncoder;

    private HashMap<String, AnnotationConfigApplicationContext> tenantSpringContextMap = new HashMap<String, AnnotationConfigApplicationContext>();

    @PostConstruct
    public void init() {
        accountCollection.createIndex(new BasicDBObject("accountName", 1).append("unique", true));
        refreshTenantSpringContexts();
    }

    @Override
    public Account getAccount(String accountName) {
        return accountRepository.findByAccountName(accountName);
    }

    @Override
    public void createAccount(String accountName, String dbType, String dbUri, String dbName, String superadminUserName, String superadminPassword) {
        accountRepository.save(new Account(accountName, dbType, dbUri, dbName, superadminUserName, superadminPassword));
        refreshTenantSpringContexts();
        String adminRole = "ROLE_ADMIN";
        String userRole = "ROLE_USER";
        roleService.createRole(accountName, adminRole);
        roleService.createRole(accountName, userRole);
        userService.createUser(accountName, superadminUserName,
                passwordEncoder.encode(superadminPassword), new ArrayList<String>(Arrays.asList(adminRole, userRole)));
    }

    @Override
    public List<? extends Account> getAccounts() {
        return accountRepository.findAll();
    }

    private void refreshTenantSpringContexts() {
        List<? extends Account> accounts = getAccounts();
        String tenantId;
        DbType tenantDbType;
        String tenantConfigClassStr;
        AnnotationConfigApplicationContext tenantSpringContext;
        for (Account account : accounts) {
            tenantId = account.getAccountName();
            tenantDbType = account.getDbType();
            switch (tenantDbType) {
                case mongo: {
                    tenantConfigClassStr = "mongo.config.MongoConfig";
                    break;
                }
                case couchDB: {
                    tenantConfigClassStr = "couchdb.config.CouchdbConfig";
                    break;
                }
                default: {
                    tenantConfigClassStr = "mongo.config.MongoConfig";
                }
            }
            try {
                tenantSpringContext = ApiConfig.createSpringContext(tenantConfigClassStr, createConfigurableEnvironment(account));
                tenantSpringContextMap.put(tenantId, tenantSpringContext);
            } catch (Exception ex) {
                Logger.info("Cannot create tenant spring context", ex);
            }
        }
        ApiConfig.tenantSpringContextMap = tenantSpringContextMap;
    }

    private ConfigurableEnvironment createConfigurableEnvironment(Account account) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        Map myMap = new HashMap();
        DbType dbType = account.getDbType();
        switch (dbType) {
            case mongo: {
                putMongoProperties(myMap, account);
                break;
            }
            case couchDB: {
                myMap.put("uri", account.getDbUri());
                break;
            }
            default: {
                putMongoProperties(myMap, account);
            }
        }
        propertySources.addFirst(new MapPropertySource("account_properties", myMap));
        return environment;
    }

    private void putMongoProperties(Map myMap, Account account) {
        myMap.put("dbname", account.getDbName());
        myMap.put("uri", account.getDbUri());
    }
}