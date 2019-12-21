package com.pushpole.sdk.internal.exceptions;

/**
 * Created on 16-03-13, 11:35 AM.
 *
 * @author Hadi Zolfaghari
 * Edit Akram Shokri
 *         custome NotificationBuildFailed exception
 */

public class NotificationBuildFailed extends Exception {
    public NotificationBuildFailed() {
    }

    public NotificationBuildFailed(String message) {
        super(message);
    }
}

