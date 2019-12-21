package com.pushpole.sdk.topic;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.regex.Pattern;

import com.pushpole.sdk.Constants;
import com.pushpole.sdk.FirebaseAppNotAvailableException;
import com.pushpole.sdk.PushPole;
import com.pushpole.sdk.internal.log.LogData;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.network.SendManager;
import com.pushpole.sdk.task.TaskManager;
import com.pushpole.sdk.task.options.TaskOptions;
import com.pushpole.sdk.task.tasks.SubscribeTask;
import com.pushpole.sdk.util.Pack;

/***
 * A helper class to subscribe and unsubscribe to GCM channel
 */
public class TopicSubscriber {
    private static final Pattern topicNameRegex = Pattern.compile("[a-zA-Z0-9-_.~%]{1,900}");
    private Context mContext;

    private final static String TOPIC_PREFIX = "/topics/";
    public TopicSubscriber(Context context) {
        mContext = context;
    }

    /***
     * Subscribe to a specific GCM channel (topic), channel will be created if not exist
     *
     * @param topicCode a specific channel name (topic code)
     * @throws IOException
     */
    public void subscribe(String topicCode) throws IOException, FirebaseAppNotAvailableException {
        String nonPrefixedTopic = topicCode.replace(TOPIC_PREFIX, "");

        PushPole.getFirebaseMessaging(mContext).subscribeToTopic(nonPrefixedTopic);
        sendTopicSubUnsubSucceedMsg(nonPrefixedTopic, true);//Sending topic status msg to server via SendManager
        Logger.debug("Topic Subscription Successful", new LogData(
                "topic", nonPrefixedTopic
        ));
        android.util.Log.i("PushPole", "Successfully subscribed to topic");
    }

    /***
     * unsubscribe from a specific existing GCM channel (topic)
     *
     * @param topicCode a specific channel name (topic code)
     * @throws IOException
     */
    public void unsubscribe(String topicCode) throws IOException, FirebaseAppNotAvailableException {
        String nonPrefixedTopic = topicCode.replace(TOPIC_PREFIX, "");
        PushPole.getFirebaseMessaging(mContext).unsubscribeFromTopic(nonPrefixedTopic);
        sendTopicSubUnsubSucceedMsg(nonPrefixedTopic, false);//Sending topic status msg to server via SendManager
        Logger.debug("Topic Unsubscription Successful", new LogData(
                "topic", nonPrefixedTopic
        ));
        android.util.Log.i("PushPole", "Successfully unsubscribed from topic");
    }

    /***
     * Subscribe to a specific GCM channel (topic),
     * channel name must not be null and match this pattern {@code topicNameRegex}
     * if subscription is unsuccessful then re-schedule subscription
     *
     * @param channel the channel name
     * @throws IllegalArgumentException
     */
    public void subscribeToChannel(String channel) {
        String pkgName = mContext.getPackageName();
        if (pkgName == null || pkgName.isEmpty()) {
            Logger.error("PushPole: Error in getting PackageName in topic subscriber. Aborting subscription.", new LogData("Package name", pkgName, "Channel name", channel));
            android.util.Log.e("PushPole", "Subscribe to topic failed");
            return;
        }

        String topic = TOPIC_PREFIX + channel + "_" + pkgName.toLowerCase();

        if (topic != null && topicNameRegex.matcher(channel).matches()) {
            try {
                Log.i("PushPole", "Trying to subscribe to topic: " + channel);
                subscribe(topic);

            } catch (IOException | IllegalArgumentException | FirebaseAppNotAvailableException e) {
                Pack data = new Pack();
                data.putString(Constants.getVal(Constants.TASK_ACTION), String.valueOf("subscribe"));
                data.putString(Constants.getVal(Constants.TOPICS), topic);
                TaskOptions taskOptions = new TaskOptions.Builder()
                        .setDelay(16 * 60  * 1000L)
                        .setWindow(60 * 1000L)
                        .build();
                TaskManager.getInstance(mContext).scheduleTask(SubscribeTask.class, data, taskOptions);
            }

        } else {
            Log.e("PushPole", "bad channel name, channel name must just contains [a-zA-Z0-9-_.~%]{1,900}");
        }
    }

    /***
     * unsubscribe from a specific GCM channel (topic)
     * if unsubscription is unsuccessful then re-schedule unsubscription
     *
     * @param channel the channel (topic) name
     */
    public void unsubscribeFromChannel(String channel) {
        String pkgName = mContext.getPackageName();
        if (pkgName == null || pkgName.isEmpty()) {
            Logger.error("PushPole: Error in getting PackageName in topic unSubscriber.  unSubscription failed.", new LogData("Package name", pkgName, "Channel name", channel));
            android.util.Log.e("PushPole", "Unsubscribe from topic failed");
            return;
        }
        String topic = TOPIC_PREFIX + channel + "_" + pkgName.toLowerCase();
        if (topic != null && topicNameRegex.matcher(channel).matches()) {
            try {
                Log.i("PushPole", "Trying to unsubscribe from topic: " + channel);
                unsubscribe(topic);

            } catch (IOException | FirebaseAppNotAvailableException e) {
                Pack data = new Pack();
                data.putString(Constants.getVal(Constants.TASK_ACTION), String.valueOf("unsubscribe"));
                data.putString(Constants.getVal(Constants.TOPICS), topic);
                TaskOptions taskOptions = new TaskOptions.Builder()
                        .setDelay(16 * 60  * 1000L)
                        .setWindow(60 * 1000L)
                        .build();
                TaskManager.getInstance(mContext).scheduleTask(SubscribeTask.class, data, taskOptions);
            }
        } else
            Log.e("PushPole", "bad channel name, channel name must just contains [a-zA-Z0-9-_.~%]{1,900}");
    }

    private void sendTopicSubUnsubSucceedMsg(String topicName, boolean isSubscribe){
        Topic aTopic;
        if(isSubscribe)
            aTopic = new Topic(topicName,  Constants.F_TOPIC_STATUS_SUBSCRIBE);
        else
            aTopic = new Topic(topicName,  Constants.F_TOPIC_STATUS_UNSUBSCRIBE);
        SendManager.getInstance(mContext).send(Constants.getVal(Constants.TOPIC_STATUS_T), aTopic.toPack());
    }
}
