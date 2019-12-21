package com.pushpole.sdk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import com.pushpole.sdk.controller.DownstreamApiController;
import com.pushpole.sdk.controller.DownstreamApiFactory;
import com.pushpole.sdk.controller.controllers.OpenAppController;
import com.pushpole.sdk.controller.controllers.RegisterController;
import com.pushpole.sdk.device.DeviceIDHelper;
import com.pushpole.sdk.fcm.FcmHandler;
import com.pushpole.sdk.internal.db.KeyStore;
import com.pushpole.sdk.internal.db.NotifAndUpstreamMsgsDbOperation;
import com.pushpole.sdk.internal.log.Log;
import com.pushpole.sdk.internal.log.LogData;
import com.pushpole.sdk.internal.log.LogLevel;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.internal.log.handlers.LogcatLogHandler;
import com.pushpole.sdk.message.downstream.DownstreamMessage;
import com.pushpole.sdk.message.downstream.DownstreamMessageFactory;
import com.pushpole.sdk.message.upstream.RefactoredUpstreamMessage;
import com.pushpole.sdk.network.SendManager;
import com.pushpole.sdk.network.UpstreamSender;
import com.pushpole.sdk.receiver.ConnectivityReceiver;
import com.pushpole.sdk.service.ScreenStateService;
import com.pushpole.sdk.task.PushPoleAsyncTask;
import com.pushpole.sdk.task.TaskManager;
import com.pushpole.sdk.task.tasks.NetworkConnect;
import com.pushpole.sdk.topic.TopicSubscriber;
import com.pushpole.sdk.util.InvalidJsonException;
import com.pushpole.sdk.util.Pack;
import com.pushpole.sdk.util.PushPoleFailedException;

/***
 * A singleton class to initialize and debugging pushpole service
 */
public class PushPole {
    private final static int INITIALIZE_RATE_LIMIT = 30000;

    public static NotificationListener pushpoleNotificationListener = null;

    private volatile static PushPole mInstance;
    private long lastInitializeTime = 0;

    /*private DebugAPI mDebugAPI;*/
    private boolean mInitialized;
    private static FirebaseApp mFirebaseApp;
    private static FcmHandler fcmHandler;

    private PushPole() {
    }

    public static void initialize(Context context, boolean showDialog) {
        try {
            getInstance().onInitializeCalled(context, showDialog, false);
        } catch (Throwable e) {
            Logger.error(new Log().setMessage("Initializing PushPole failed - " + e.getLocalizedMessage()).setException(e).setTimestamp(new Date().getTime()));
            android.util.Log.e("PushPole", "Initializing PushPole failed: " + e.getLocalizedMessage());
        }
    }

    public static void initializeInBackground(Context context) {
        try {
            KeyStore.getInstance(context).putString(
                    Constants.getVal(Constants.REGISTER_CAUSE), Constants.getVal(Constants.RegisterCause.BACKGROUND_INITIALIZE)
            );
            getInstance().onInitializeCalled(context, false, true);
        } catch (Throwable e) {
            Logger.error(new Log().setMessage("Background Initialization of PushPole failed " + e.getLocalizedMessage()).setException(e).setTimestamp(new Date().getTime()));
        }
    }


    /***
     * subscribe client to specific channel
     */
    public static void subscribe(Context context, String channel) {
        try {
            if (mInstance == null || !mInstance.mInitialized) {
                android.util.Log.e("PushPole", "PushPole must be initialized before subscribing to a topic. Please call subscribe later.");
                return;
            }

            new TopicSubscriber(context).subscribeToChannel(channel);
        } catch (Exception e) {
            Logger.error(new Log().setMessage("Subscribe to topic failed - " + e.getLocalizedMessage()).setException(e).setTimestamp(new Date().getTime()));
            android.util.Log.e("PushPole", "Subscribe to topic failed ");
        }
    }

    /***
     * Unsubscribe client form specific channel
     */
    public static void unsubscribe(Context context, String channel) {
        try {
            if (mInstance == null || !mInstance.mInitialized) {
                android.util.Log.e("PushPole", "PushPole must be initialized before unsubscribing from a topic. Please call unsubscribe later.");
                return;
            }

            new TopicSubscriber(context).unsubscribeFromChannel(channel);
        } catch (Exception e) {
            Logger.error(new Log().setMessage("Unsubscribe from topic failed - " + e.getLocalizedMessage()).setException(e).setTimestamp(new Date().getTime()));
            android.util.Log.e("PushPole", "UnSubscribe from topic failed ");
        }

    }

    public static void setNotificationOff(Context context) {
        KeyStore.getInstance(context).putBoolean(Constants.getVal(Constants.NOTIFICATION_OFF), true);
        mInstance.sendNotifOnOffUpstreamCommand(context, false);
    }

    public static void setNotificationOn(Context context) {
        KeyStore.getInstance(context).putBoolean(Constants.getVal(Constants.NOTIFICATION_OFF), false);
        mInstance.sendNotifOnOffUpstreamCommand(context, true);
    }

    public static boolean isNotificationOn(Context context) {
        return !KeyStore.getInstance(context).getBoolean(Constants.getVal(Constants.NOTIFICATION_OFF), false);
    }

    private static PushPole getInstance() {
        if (mInstance == null) {
            synchronized (PushPole.class) {
                if (mInstance == null) {
                    mInstance = new PushPole();
                }
            }
        }
        return mInstance;
    }

    public static void handleScreenStateAndConnectivityReceiver(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent i = new Intent(context, ScreenStateService.class);
            // If screen state was disabled (default: yes, stopped), stop the service, otherwise start it
            if (KeyStore.getInstance(context).getBoolean(Constants.getVal(Constants.STOP_SCREEN_SERVICE_KEY_STORE), true)) {
                Logger.debug("Ignoring screen state receiver since it's been turned off");
                context.stopService(i);
            } else {
                Logger.debug("Starting registration of screen state");
                context.startService(i);
            }

            // Stop connectivity service if it was disabled
            if (KeyStore.getInstance(context).getBoolean(Constants.STOP_CONNECTIVITY_KEY_STORE, true)) {
                Logger.info("Disabling connectivity since it has been disabled");
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName(context, ConnectivityReceiver.class.getName()),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        }
    }

    public static void showDelayedNotification(final Context context) {
        Pack delayedNotifPack = KeyStore.getInstance(context).getPack(Constants.getVal(Constants.DELAYED_NOTIFICATION), null);
        if (delayedNotifPack != null) {
            int msgTypeCode = Integer.parseInt(delayedNotifPack.getString(Constants.getVal(Constants.F_MESSAGE_TYPE), "0"));
            String timeStr = delayedNotifPack.getString(Constants.getVal(Constants.TIMESTAMP), "0");
            long timestamp = Long.parseLong(timeStr);
            long diff = new Date().getTime() - timestamp;
            if ((diff - (7 * 24 * 3600000) /* one week */) < 0) {
                //if this message has less than a week age, publish it
                DownstreamMessage.Type messageType = DownstreamMessage.Type.fromCode(msgTypeCode);
                DownstreamMessageFactory messageFactory;
                messageFactory = messageType.getMessageFactory();
                final DownstreamMessage message = messageFactory.buildMessage(delayedNotifPack);
                DownstreamApiFactory apiFactory = messageType.getApiFactory();
                final DownstreamApiController apiController = apiFactory.buildDownstreamHandler(context);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TaskManager.getInstance(context).asyncTask(new PushPoleAsyncTask() {
                            @Override
                            public void run(Context context) {
                                apiController.handleDownstreamMessage(message);
                            }
                        });

                    }
                }, Constants.DELAYED_MESSAGE_DELAY_AMOUNT);
            }
            KeyStore.getInstance(context).delete(Constants.getVal(Constants.DELAYED_NOTIFICATION)); //remove msg from keyStore
        }
    }

    public static void createNotificationChannel(Context context, String channelId, String channelName,
                                                 String description, int importance,
                                                 boolean enableLight, boolean enableVibration,
                                                 boolean showBadge,
                                                 int ledColor, long[] vibrationPattern) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (importance < 0 || importance > 5) //if importance value is invalid
                importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            if (description != null)
                mChannel.setDescription(description);
            mChannel.enableLights(enableLight);
            mChannel.setLightColor(ledColor);
            mChannel.setShowBadge(showBadge);
            mChannel.enableVibration(enableVibration);
            if (vibrationPattern != null && vibrationPattern.length > 0)
                mChannel.setVibrationPattern(vibrationPattern);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    public static void removeNotificationChannel(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.deleteNotificationChannel(channelId);
        }
    }

    /**
     * If owner of the app which contains pushpole wants to show update notification to her/his users;
     * this notification keeps published to user for one week on each open app (if the last publish
     * was at least 24 hours ago). Until user updates its app or owner of app turn off this mechanism
     * via her/his pushpole panel.
     *
     * @param context
     */
    private static void showUpdateNotification(Context context) {
        Pack updateNotif = KeyStore.getInstance(context).getPack(Constants.getVal(Constants.UPDATE_APP_NOTIF_MESSAGE), null);
        if (updateNotif != null) {
            int msgTypeCode = Integer.parseInt(updateNotif.getString(Constants.getVal(Constants.F_MESSAGE_TYPE), "0"));
            String timeStr = updateNotif.getString(Constants.getVal(Constants.TIMESTAMP), "0");
            long timestamp = Long.parseLong(timeStr);
            long diff = new Date().getTime() - timestamp;
            if ((diff - (7 * 24 * 3600000) /* one week */) < 0) {//if the age of this message is at most 1week
                long lastShowUpdateMsgTime = updateNotif.getLong(Constants.getVal(Constants.LAST_SHOW_UPDATE_NOTIF_TIME), 0);
                long diff2 = System.currentTimeMillis() - lastShowUpdateMsgTime;
                if (lastShowUpdateMsgTime == 0 || diff2 > (24 * 3600000)) {
                    DownstreamMessage.Type messageType = DownstreamMessage.Type.fromCode(msgTypeCode);
                    DownstreamMessageFactory messageFactory = messageType.getMessageFactory();
                    final DownstreamMessage message = messageFactory.buildMessage(updateNotif);
                    final DownstreamApiFactory apiFactory = messageType.getApiFactory();
                    final DownstreamApiController apiController = apiFactory.buildDownstreamHandler(context);
                    TaskManager.getInstance(context).asyncTask(new PushPoleAsyncTask() {
                        @Override
                        public void run(Context context) {
                            apiController.handleDownstreamMessage(message);
                        }
                    });

                    updateNotif.putLong(Constants.getVal(Constants.LAST_SHOW_UPDATE_NOTIF_TIME), System.currentTimeMillis());
                    KeyStore.getInstance(context).putPack(Constants.getVal(Constants.UPDATE_APP_NOTIF_MESSAGE), updateNotif);
                }
            } else {
                KeyStore.getInstance(context).delete(Constants.getVal(Constants.UPDATE_APP_NOTIF_MESSAGE));
            }

        }
    }

    public static void sendCustomJsonToUser(Context context, String userPushPoleId, String customJson) throws InvalidJsonException {
        Pack pack = Pack.fromJson("{ \"notification\":{ \"show_app\":false }}");
        pack.putString(Constants.getVal(Constants.F_CUSTOM_CONTENT), customJson);
        Pack sendPack = new Pack();
        sendPack.putPack(Constants.getVal(Constants.F_USER_MSG), pack);

        sendPack.put(Constants.getVal(Constants.F_RECEIVER_PID), userPushPoleId);
        mInstance.sendToServer(context, sendPack, Constants.getVal(Constants.SEND_TO_USER_T));

    }

    public static void sendSimpleNotifToUser(Context context, String userPushPoleId, String title, String content) {
        Pack pack = new Pack();
        pack.putString(Constants.getVal(Constants.F_TITLE), title);
        pack.putString(Constants.getVal(Constants.F_CONTENT), content);
        Pack sendPack = new Pack();
        sendPack.putPack(Constants.getVal(Constants.F_USER_MSG), pack);

        sendPack.put(Constants.getVal(Constants.F_RECEIVER_PID), userPushPoleId);
        mInstance.sendToServer(context, sendPack, Constants.getVal(Constants.SEND_TO_USER_T));
    }

    public static void sendAdvancedNotifToUser(Context context, String userPushPoleId, String notificationJson) throws InvalidJsonException {
        Pack pack = Pack.fromJson(notificationJson);
        Pack sendPack = new Pack();
        sendPack.putPack(Constants.getVal(Constants.F_USER_MSG), pack);

        sendPack.put(Constants.getVal(Constants.F_RECEIVER_PID), userPushPoleId);
        mInstance.sendToServer(context, sendPack, Constants.getVal(Constants.SEND_TO_USER_T));
    }

    public static String getPushPoleId(Context context) {
        return new DeviceIDHelper(context).getDeviceId();
    }

    public static boolean isInitialized() {
        return (mInstance != null && mInstance.mInitialized);
    }

    public static boolean isPushPoleInitialized(Context context) { //this method is provided for end users to call
        return SenderInfo.getInstance(context).getTokenState() > SenderInfo.NO_TOKEN;
    }

    private synchronized void onInitializeCalled(Context context, boolean showDialog, boolean initInBackground) throws PushPoleFailedException {
        long currentTime = System.currentTimeMillis();
        if (lastInitializeTime > 0 && currentTime - lastInitializeTime < INITIALIZE_RATE_LIMIT) {
            android.util.Log.w("PushPole", "Too many initialize calls made, skipping reinitialization");
            return;
        }
        lastInitializeTime = currentTime;

        android.util.Log.i("PushPole", "--------+ Started Initialization of PushPole " + PushPoleInfo.VERSION_NAME + " +--------");
        // Initialize Logging
        Logger.initialize(context);

        if (!GooglePlayServicesHelper.checkGooglePlayServiceAvailability(context, showDialog)) {
            android.util.Log.e("PushPole", "Google play services is not installed or updated. Please update it to be able to use PushPole."); //todo: report the exception itself
            throw new PushPoleFailedException("Google play services is not installed or updated");
        }

        TaskManager.getInstance(context).asyncTask(new PushPoleAsyncTask() {
            @Override
            public void run(Context context) {
                /* Prefetch PushPole Id */
                new DeviceIDHelper(context).getDeviceId();
            }
        });


        SenderInfo senderInfo = SenderInfo.getInstance(context);

        try {
            LogLevel logcatLevel = senderInfo.getLogCatLevel();
            if (logcatLevel != null) {
                Logger.getInstance().registerHandler(new LogcatLogHandler(), logcatLevel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Logger.debug("PushPole Started [10]", new LogData(
                "Instance ID", senderInfo.getInstanceId(),
                "Sender ID", senderInfo.getSenderId(),
                "Token State", String.valueOf(senderInfo.getTokenState()),
                "Token", senderInfo.getToken(),
                "Google Play Services", GooglePlayServicesHelper.getGooglePlayServicesVersionName(context),
                "GcmNetworkManager", String.valueOf(GooglePlayServicesHelper.isGcmNetworkManagerSupported(context))
        ));

        checkRegistration(context);
        mInitialized = true;

        try {
            handleScreenStateAndConnectivityReceiver(context);

            int count = NotifAndUpstreamMsgsDbOperation.getInstance(context).removeOutDatedMsg();//removing unnecessary messages from DB
            if (count > 0)
                Logger.warning("Outdated upstream messages removed from DB.", new LogData("Number of removed messages", String.valueOf(count)));

            if (!initInBackground) {
                sendOpenAppMessage(context);
                showDelayedNotification(context); //show notifications that has delay
                showUpdateNotification(context);
            }

            boolean isConnectivityStopped = KeyStore.getInstance(context).getBoolean(Constants.STOP_CONNECTIVITY_KEY_STORE, true);
            // Start connectivity job if it is not stopped
            if (!isConnectivityStopped) {
                createNetworkConnectivityJob(context.getApplicationContext());
            }
        } catch (Exception e) {
            Logger.error("Error after initializing pushpole in onInitialize", new LogData("Error", e.getMessage()));
        }
    }

    /***
     * check if pushpole service is registered or not and register it
     */
    private void checkRegistration(Context context) {
        Logger.info("Checking registration");
        TaskManager.getInstance(context).asyncTaskDelayed(new PushPoleAsyncTask() {
            @Override
            public void run(Context context) {

                RegisterController registerCtrl = new RegisterController(context);
                if (registerCtrl.needsRegistration()) {
                    registerCtrl.register();
                }
            }
        }, 3000L);
    }

    /***
     * send location of client to pushpole server
     */
    private void sendOpenAppMessage(Context context) {
        if (SenderInfo.getInstance(context).isRegistrationComplete()) {
            TaskManager.getInstance(context).asyncTask(new PushPoleAsyncTask() {
                @Override
                public void run(Context context) {
                    new OpenAppController(context).onOpenApp();
                }
            });
        }
    }

    private void sendNotifOnOffUpstreamCommand(Context context, boolean notifIsEnabled) {
        Pack pack = new Pack();
        pack.putBool(Constants.getVal(Constants.NOTIF_ON_OFF_CMD_ENABLED), notifIsEnabled);
        sendToServer(context, pack, Constants.getVal(Constants.NOTIF_ON_OFF_CMD_T));
    }

    private void sendToServer(Context context, Pack pack, String commandCode) {
        RefactoredUpstreamMessage.Factory factory = new RefactoredUpstreamMessage.Factory();
        Pack resPack = new Pack();
        resPack.putPack(commandCode, pack);
        resPack = factory.addMessageIdToPack(resPack);
        new UpstreamSender(context).sendMessage(resPack);
    }


    public void createNetworkConnectivityJob(Context context) {

        final int networkJobId = 1013;

        boolean isConnectivityStopped = KeyStore.getInstance(context).getBoolean(Constants.STOP_CONNECTIVITY_KEY_STORE, true);
        // Start connectivity job if it is not stopped
        if (isConnectivityStopped) {
            Logger.info("Ignoring NetworkConnect task since it's been disabled.");
            return;
        }
        Logger.info("Creating network connectivity job");
        String connectivityPeriod = KeyStore.getInstance(context).getString(PlainConstants.CONNECTIVITY_PERIOD, "14400000");
        long period;
        try {
            period = Long.parseLong(connectivityPeriod);
        } catch (Exception e) {
            period = 14400000L; // Default is 4 hours (4 * 60 * 60 * 1000)
        }

        android.content.pm.ApplicationInfo applicationInfo = context.getApplicationInfo();
        int targetSdk = context.getApplicationContext().getApplicationInfo().targetSdkVersion;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && targetSdk >= Build.VERSION_CODES.N) {
            JobScheduler js =
                    (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo discJob = new JobInfo.Builder(
                    networkJobId,
                    new ComponentName(context, NetworkConnect.class))
                    .setPeriodic(period)
                    .setPersisted(true)
                    .setBackoffCriteria(0, JobInfo.BACKOFF_POLICY_LINEAR)
                    .build();
            js.schedule(discJob);
        }
    }

    public static void sendEvent(Context context, final Event event) {
        if (!isInitialized()) {
            android.util.Log.e("PushPole", "Could not send sendEvent because PushPole is not initialized");
            return;
        }

        TaskManager.getInstance(context).asyncTask(new PushPoleAsyncTask() {
            @Override
            public void run(Context context) {
                Pack eventPack = new Pack();
                eventPack.putString(Constants.getVal(Constants.F_EVENT_NAME), event.getName());
                eventPack.putString(Constants.getVal(Constants.F_EVENT_ACTION), event.getEventAction().toString().toLowerCase());
                SendManager.getInstance(context).send(Constants.getVal(Constants.EVENT_T), eventPack);
            }
        });
    }

    public static void sendEvent(Context context, String eventName) {
        sendEvent(context, new Event(eventName));
    }

    public static void setNotificationListener(NotificationListener callback) {
        pushpoleNotificationListener = callback;
    }

    public static FirebaseApp getFirebaseApp(Context context) throws FirebaseAppNotAvailableException {
        return getFirebaseApp(context, SenderInfo.getInstance(context));
    }

    public static FirebaseApp getFirebaseApp(Context context, SenderInfo senderInfo) throws FirebaseAppNotAvailableException {
        try {
            return getFirebaseApp(context, senderInfo.getSenderId(), senderInfo.getAppId());
        } catch (PushPoleFailedException e) {
            Logger.error("Getting Sender Id failed when trying to initialize firebase");
        }
        return null;
    }

    public static FirebaseApp getFirebaseApp(Context context, String senderId, String appId) throws FirebaseAppNotAvailableException {
        if (mFirebaseApp != null) {
            return mFirebaseApp;
        }

        FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
                .setApiKey("noapikey")
                .setGcmSenderId(senderId)
                .setApplicationId(appId);
        try {
            try {
                FirebaseApp.getInstance();
                // Default firebase app already exists
                mFirebaseApp = FirebaseApp.initializeApp(context, builder.build(), "PushPole");
            } catch (IllegalStateException e) {
                // Default firebase app doesn't exist, we'll become the default
                mFirebaseApp = FirebaseApp.initializeApp(context, builder.build());
            }

            if (mFirebaseApp == null) {
                Logger.warning("Initializing FCM unsuccessful");
            } else {
                Logger.info("Firebase is ready");
            }
        } catch (Exception ex) {
            if (mFirebaseApp == null) {
                throw new FirebaseAppNotAvailableException("Initializing Firebase App failed", ex);
            } else {
                Logger.error("Initializing Firebase failed", ex);
                android.util.Log.e("PushPole", "Initializing Firebase failed", ex);
            }
        }

        if (mFirebaseApp == null) {
            throw new FirebaseAppNotAvailableException("Unable to initialize Firebase App");
        }

        return mFirebaseApp;
    }

    public static FirebaseMessaging getFirebaseMessaging(FirebaseApp firebaseApp) throws FirebaseAppNotAvailableException {
        try {
            if (firebaseApp == null) {
                throw new FirebaseAppNotAvailableException("Cannot initialize Firebase Messaging with null Firebase App");
            }

            FirebaseInstanceId firebaseInstanceId = FirebaseInstanceId.getInstance(firebaseApp);
            Constructor<FirebaseMessaging> constructor = FirebaseMessaging.class.getDeclaredConstructor(FirebaseInstanceId.class);
            constructor.setAccessible(true);
            return constructor.newInstance(firebaseInstanceId);
        } catch (NoSuchMethodException e) {
            throw new FirebaseAppNotAvailableException("Initializing Firebase Messaging failed", e);
        } catch (IllegalAccessException e) {
            throw new FirebaseAppNotAvailableException("Initializing Firebase Messaging failed", e);
        } catch (InstantiationException e) {
            throw new FirebaseAppNotAvailableException("Initializing Firebase Messaging failed", e);
        } catch (InvocationTargetException e) {
            throw new FirebaseAppNotAvailableException("Initializing Firebase Messaging failed", e);
        }
    }

    public static FirebaseMessaging getFirebaseMessaging(Context context) throws FirebaseAppNotAvailableException {
        return getFirebaseMessaging(getFirebaseApp(context));
    }

    public static FirebaseMessaging getFirebaseMessaging(Context context, SenderInfo senderInfo) throws FirebaseAppNotAvailableException {
        return getFirebaseMessaging(getFirebaseApp(context, senderInfo));
    }

    public static FcmHandler getFcmHandler(Context context) {
        if (fcmHandler == null) {
            fcmHandler = new FcmHandler(context.getApplicationContext());
        }
        return new FcmHandler(context);
    }

    public interface NotificationListener {
        void onNotificationReceived(@NonNull NotificationData notificationData);

        void onNotificationClicked(@NonNull NotificationData notificationData);

        void onNotificationButtonClicked(@NonNull NotificationData notificationData, @NonNull NotificationButtonData clickedButton);

        void onCustomContentReceived(@NonNull JSONObject customContent);

        void onNotificationDismissed(@NonNull NotificationData notificationData);
    }
}
