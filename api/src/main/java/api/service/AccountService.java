package api.service;

import api.domain.Account;

import java.util.List;

public interface AccountService {

    Account getAccount (String accountName);

    void createAccount (String accountName, String dbType, String dbUri, String dbName, String superadminUserName, String superadminPassword);

    List<? extends Account> getAccounts();
}
