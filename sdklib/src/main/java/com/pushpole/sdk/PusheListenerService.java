package com.pushpole.sdk;


import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import com.pushpole.sdk.util.Pack;
import com.pushpole.sdk.util.PackBundler;

/***
 * A service that listen for new downstream content and extra JSON, In order to receive content and extra JSON
 * of message you should create a subclass of {@link PushPoleListenerService} class and implement it's onMessageReceived method
 */
public class PushPoleListenerService extends Service {

    /**
     * TODO: PAS-3:set minsdk=8
     * Below method is modified to be compatible with sdk<11
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    private Intent mIntent;
    //private int mFlags; //unused variable
    private int mStartId;

    /***
     * A call back when new downstream received
     *
     * @param json           extra json in message that could be null
     * @param messageContent content of downstream message such as title, bigTitle
     */
    public void onMessageReceived(JSONObject json, JSONObject messageContent) {

    }

    /**
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    //TODO: #PAS-3, set minsdk=9 : This method has changed to support minsdk=9
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        mIntent = intent;
        //mFlags = flags;
        mStartId = startId;

        PushPoleListenerAsyncTask pushpoleListenerAsyncTask = new PushPoleListenerAsyncTask();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            pushpoleListenerAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {

            pushpoleListenerAsyncTask.execute();
        }

        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @author Akram Shokri
     *         This class is defined to be used in @link{PushPoleListenerService.onStartCommand()} mehtod to support
     *         minsdk = 9
     */
    public class PushPoleListenerAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Pack json;
            // Check if any json available
            try {
                json = PackBundler.bundleToPack(mIntent.getBundleExtra("json"));//custom json
            } catch (Exception e) {
                // Make empty json
                json = new Pack();
            }
            Pack messageContent = PackBundler.bundleToPack(mIntent.getBundleExtra("messageContent"));//notification itself
            onMessageReceived(json.toJsonObject(), messageContent.toJsonObject());
            stopSelfResult(mStartId);
            return null;
        }
    }
}
