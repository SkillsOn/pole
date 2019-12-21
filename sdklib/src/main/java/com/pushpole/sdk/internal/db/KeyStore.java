package com.pushpole.sdk.internal.db;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

import com.pushpole.sdk.util.InvalidJsonException;
import com.pushpole.sdk.util.ListPack;
import com.pushpole.sdk.util.Pack;

public class KeyStore {
    private final static String SHARED_PREF_NAME = "com.pushpole.sdk.keystore";
    //private final static String ACTIVE_PACKS_KEY = "pack___tags"; //unused field
    //private final static String DUMMY_KEY = "DUM#MY"; //unused field

    private volatile static KeyStore mInstance;

    private SharedPreferences mSharedPrefs;

    private KeyStore(Context context) {//TODO: check context to be not null
        mSharedPrefs = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static KeyStore getInstance(Context context) {
        if (mInstance == null) {
            synchronized (KeyStore.class) {
                if (mInstance == null) {
                    mInstance = new KeyStore(context);
                }
            }
        }
        return mInstance;
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPrefs;
    }

    /**
     * This method is not used anywhere in this lib. It is commented to support compatibility with min-sdk=9
     *
     * @param key
     * @param defVal
     * @return Set<String>
     */
    /*public Set<String> getStringSet(String key, Set<String> defVal) {
            return mSharedPrefs.getStringSet(key, defVal);
    }*/
    public String getString(String key, String defVal) {
        return mSharedPrefs.getString(key, defVal);
    }

    public int getInt(String key, int defVal) {
        return mSharedPrefs.getInt(key, defVal);
    }

    public synchronized long getLong(String key, long defVal) {
        return mSharedPrefs.getLong(key, defVal);
    }

    public Map<String, ?> getAll() {
        return mSharedPrefs.getAll();
    }

    public boolean getBoolean(String key, boolean defVal) {
        return mSharedPrefs.getBoolean(key, defVal);
    }

    public Pack getPack(String key, Pack defValue) {
        String pVal = getString(key, null);
        if (pVal == null) {
            return defValue;
        }

        try {
            return Pack.fromJson(pVal);
        } catch (InvalidJsonException e) {
            return null;
        }
    }

    public Pack getPack(String key) {
        return getPack(key, null);
    }

    public ListPack getListPack(String key, ListPack defValue) {
        String pVal = getString(key, null);
        if (pVal == null) {
            return defValue;
        }

        try {
            return ListPack.fromJson(pVal);
        } catch (InvalidJsonException e) {
            return null;
        }
    }

    public ListPack getListPack(String key) {
        return getListPack(key, null);
    }

    public void putString(String key, String value) {
        mSharedPrefs.edit().putString(key, value).apply();
    }

    public void putInt(String key, int value) {
        mSharedPrefs.edit().putInt(key, value).apply();
    }

    public synchronized void putLong(String key, long value) {
        mSharedPrefs.edit().putLong(key, value).apply();
    }

    public void putBoolean(String key, boolean value) {
        mSharedPrefs.edit().putBoolean(key, value).apply();
    }

    /**
     * * This method is not used anywhere in this lib. It is commented to support compatibility with min-sdk=9
     *
     * @param key
     */
    /*public void putStringSet(String key, Set<String> value) {
        mSharedPrefs.edit().putStringSet(key, value).apply();
    }*/
    public void putPack(String key, Pack pack) {
        mSharedPrefs.edit().putString(key, pack.toJson()).apply();
    }

    public void putListPack(String key, ListPack listPack) {
        mSharedPrefs.edit().putString(key, listPack.toJson()).apply();
    }

    /**
     * This method is not used anywhere in this lib. It is commented to support compatibility with min-sdk=9
     *
     * @param key
     */

   /* public void updateStringSet(String key, Set<String> value) {
        // Dummy key forces rewrite of the string set (same references prevent rewrite otherwise)
        // http://stackoverflow.com/questions/7057845/save-arraylist-to-sharedpreferences
        mSharedPrefs.edit()
                .putInt(DUMMY_KEY, IdGenerator.generateIntegerId())
                .putStringSet(key, value).apply();
    }*/
    public boolean contains(String key) {
        return mSharedPrefs.contains(key);
    }

    public void delete(String key) {
        mSharedPrefs.edit().remove(key).apply();
    }

    public int size() {
        return mSharedPrefs.getAll().size();
    }

}
