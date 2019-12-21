package com.pushpole.sdk.provider;

import android.content.Context;
import android.support.v4.content.FileProvider;

import com.pushpole.sdk.internal.db.KeyStore;
import com.pushpole.sdk.internal.log.ExceptionCatcher;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.internal.log.Sentry;

import static com.pushpole.sdk.PlainConstants.USER_DSN_URL_KEYSTORE;

/**
 * Created on 2017-11-06, 3:29 PM.
 *
 * @author Akram Shokri
 */

public class PushPoleProvider extends FileProvider {

    @Override
    public boolean onCreate() {
        super.onCreate();
        try {
            Context context = getContext();
            if (context != null) {
                ExceptionCatcher.makePushPoleDefaultExceptionCatcher(context.getApplicationContext());

                String sentryDsn = KeyStore.getInstance(context).getString(USER_DSN_URL_KEYSTORE, "");
                if (sentryDsn != null && !sentryDsn.isEmpty())
                    Sentry.init(context, sentryDsn);
            }
        } catch (Exception ex) {
            Logger.error("Error occurred in PushPoleProvider", ex);
            android.util.Log.e("PushPole", "Error occurred in PushPoleProvider", ex);
        }
        return true;
    }
}
