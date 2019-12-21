package com.pushpole.sdk.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.pushpole.sdk.controller.controllers.RegisterController;
import com.pushpole.sdk.internal.log.ExceptionCatcher;
import com.pushpole.sdk.internal.log.LogData;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.network.GcmHandler;

public class FcmService extends FirebaseMessagingService {
    private FcmHandlerImpl mFcmHandler;

    public FcmService() {
        mFcmHandler = new FcmHandlerImpl(this);
    }

    @Override
    public void onNewToken(String token) {
        mFcmHandler.onNewToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        mFcmHandler.onMessageReceived(remoteMessage);
    }

    @Override
    public void onDeletedMessages() {
        mFcmHandler.onDeletedMessages();
    }

    @Override
    public void onMessageSent(String msgId) {
        mFcmHandler.onMessageSent(msgId);
    }

    @Override
    public void onSendError(String msgId, Exception error) {
        mFcmHandler.onSendError(msgId, error);
    }
}
