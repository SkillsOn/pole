package com.pushpole.sdk.message;

import com.pushpole.sdk.Constants;
import com.pushpole.sdk.util.Pack;

/***
 * Base class for message
 * every message should have unique ID
 */
public abstract class Message {
    public final static int STATUS_NONE = -1;
    public final static int STATUS_SUCCESS = 0;
    public final static int STATUS_FAILED = 1;

    private String mMessageId;

    /***
     * return the message direction
     *
     * @return {@code MessageDirection.DOWNSTREAM} or {@code MessageDirection.DOWNSTREAM}
     */
    public abstract MessageDirection getMessageDirection();

    /***
     * convert to {@link Pack}
     *
     * @return
     */
    public Pack toPack() {
        Pack pack = new Pack();
        pack.putString(Constants.getVal(Constants.F_MESSAGE_ID), mMessageId);
        return pack;
    }

    /***
     * return message ID
     *
     * @return the unique ID
     */
    public String getMessageId() {
        return mMessageId;
    }

    /***
     * set message ID
     *
     * @param messageId the unique message id
     */
    public void setMessageId(String messageId) {
        mMessageId = messageId;
    }

}
