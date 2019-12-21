# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/Merka/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ----- Removing log from release  -----
-dontskipnonpubliclibraryclasses
-forceprocessing

-assumenosideeffects class com.pushpole.sdk.internal.log.Logger {
      *** <fields>;
      *** <methods>;
}

-assumenosideeffects class com.pushpole.sdk.internal.log.LogData {
      *** <fields>;
      *** <methods>;
}

-assumenosideeffects class com.pushpole.sdk.internal.log.handlers.SentryHandler {
      *** <fields>;
      *** <methods>;
}

-assumenosideeffects class com.joshdholtz.sentry.Sentry {
      *** <fields>;
      *** <methods>;
}

-assumenosideeffects class com.pushpole.sdk.internal.log.** {
      *** <fields>;
      *** <methods>;
}

# !!! important !!! do not uncomment below code, it interfer with different pushpole tasks and prevent app from being registered to pushpole and receiving notifs
#-assumenosideeffects class com.pushpole.sdk.task.tasks.SentryReportTask$Factory {  *; }
#-assumenosideeffects class com.pushpole.sdk.task.tasks.SentryReportTask {  *; }

# ----------------------------------------

#Ronash proguard
-keepattributes Exceptions

-keep public class com.pushpole.sdk.PushPole {
    public static void initialize(android.content.Context, boolean);
    public static void subscribe(android.content.Context, java.lang.String);
    public static void unsubscribe(android.content.Context, java.lang.String);
    public static void setNotificationOff(android.content.Context);
    public static void setNotificationOn(android.content.Context);
    public static boolean isNotificationOn(android.content.Context);
    public static boolean isPushPoleInitialized(android.content.Context);
    public static java.lang.String getPushPoleId(android.content.Context);
    public static void sendCustomJsonToUser(android.content.Context, java.lang.String, java.lang.String);
    public static void sendSimpleNotifToUser(android.content.Context, java.lang.String, java.lang.String, java.lang.String);
    public static void sendAdvancedNotifToUser(android.content.Context, java.lang.String, java.lang.String);
    public static void createNotificationChannel(android.content.Context, java.lang.String, java.lang.String, java.lang.String, int, boolean, boolean, boolean, int, long[]);
    public static void removeNotificationChannel(android.content.Context, java.lang.String);
    public static void sendEvent(android.content.Context, java.lang.String);
    public static void sendEvent(android.content.Context, com.pushpole.sdk.Event);
    public static FcmHandler getFcmHandler(android.content.Context);
    public static com.google.firebase.FirebaseApp getFirebaseApp(android.content.Context);
    public static com.google.firebase.messaging.FirebaseMessaging getFirebaseMessaging(android.content.Context);
    public static com.pushpole.sdk.fcm.FcmHandler getFcmHandler(android.content.Context);
    public static void setNotificationListener(com.pushpole.sdk.PushPole$NotificationListener);
}

-keep public class com.pushpole.sdk.fcm.FcmHandler { *; }

-keep class com.pushpole.sdk.Event$* {
    *;
}

-keep class com.pushpole.sdk.PushPole$NotificationListener { *; }
-keep class com.pushpole.sdk.NotificationData { *; }
-keep class com.pushpole.sdk.NotificationButtonData { *; }

-keep class com.pushpole.sdk.Event { *; }
-keep class com.pushpole.sdk.EventAction { *; }


-keep public class com.pushpole.sdk.PushPoleListenerService {
    public void onMessageReceived(org.json.JSONObject, org.json.JSONObject);
}

-keep public class com.pushpole.sdk.receiver.UpdateReceiver
-keep public class com.pushpole.sdk.receiver.PushPoleGcmReceiver

# google gms proguard
-keep public class com.google.android.gms.**
-dontwarn com.google.android.gms.**

# google paly services proguard
-keep class * extends java.util.ListResourceBundle {
    protected java.lang.Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;}

#below keep is just for PushPolePlugin (PushPole B4A Wrapper), no need to export it for pushpole users
-keep public class com.pushpole.sdk.Constants {
    public static final java.lang.String F_CUSTOM_CONTENT;
}

#evernote Job scheduler
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper

-keep public class com.evernote.android.job.v21.PlatformJobService
-keep public class com.evernote.android.job.v14.PlatformAlarmService
-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
-keep public class com.evernote.android.job.JobBootReceiver
-keep public class com.evernote.android.job.JobRescheduleService

-keep public class com.pushpole.sdk.provider.PushPoleProvider { *; }

-keep public class com.pushpole.sdk.receiver.AppChangeReceiver { *; }

# For Sentry handler
-keep public class com.pushpole.sdk.internal.log.handlers.SentryHandler { *; }

# Exceptions
-keep public class com.pushpole.sdk.util.InvalidJsonException { *; }