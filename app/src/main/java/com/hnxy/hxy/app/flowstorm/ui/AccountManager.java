package com.hnxy.hxy.app.flowstorm.ui;

import android.app.Activity;
import android.content.Context;

import com.hnxy.hxy.app.flowstorm.R;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by hu60 on 15-8-19.
 */
public class AccountManager {
    private static AccountManager instance = null;
    private Context context;

    private AccountManager(Context context) {
        this.context = context;
    }

    public static AccountManager getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManager(context);
        }

        return instance;
    }

    public Account loadAccount() {
        Account account = null;

        try {
            ObjectInputStream file = new ObjectInputStream(context.openFileInput("account"));
            account = (Account) file.readObject();
        } catch (IOException | ClassNotFoundException e) {
            account = new Account();
        }

        return account;
    }

    protected void saveAccount(Account account) throws AccountException {

        try {
            ObjectOutputStream file = new ObjectOutputStream(context.openFileOutput("account", Activity.MODE_PRIVATE));
            file.writeObject(account);
            file.close();
        } catch (IOException e) {
            throw new AccountException(context.getString(R.string.account_save_failed, e.getMessage()));
        }
    }

    public static class AccountException extends Exception {
        public AccountException(String msg) {
            super(msg);
        }
    }
}
