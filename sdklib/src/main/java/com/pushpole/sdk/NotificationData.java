package com.pushpole.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.pushpole.sdk.util.ListPack;
import com.pushpole.sdk.util.Pack;

import static com.pushpole.sdk.PlainConstants.*;

/**
 * This is a data class used to notify user from different events of a notification.
 * Every callback contains one instance of this class, so the user does not need to parse json by himself.
 */
public class NotificationData {
    private String title, content, bigTitle, bigContent, summary, imageUrl, iconUrl, customContent;
    private List<NotificationButtonData> buttons;

    public NotificationData(String title, String content, String bigTitle, String bigContent, String summary, String imageUrl, String iconUrl, String customContent, List<NotificationButtonData> buttons) {
        this.title = title;
        this.content = content;
        this.bigTitle = bigTitle;
        this.bigContent = bigContent;
        this.summary = summary;
        this.imageUrl = imageUrl;
        this.iconUrl = iconUrl;
        this.customContent = customContent;
        this.buttons = buttons;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getBigTitle() {
        return bigTitle;
    }

    public String getBigContent() {
        return bigContent;
    }

    public String getSummary() {
        return summary;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public JSONObject getCustomContent() {
        try {
            return new JSONObject(customContent);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<NotificationButtonData> getButtons() {
        return buttons;
    }

    @Override
    public String toString() {
        return toPack().toJson();
    }

    /**
     * To transform it quickly, we can use {@link Pack}, or Parcelable (which is not used at this scope)
     * @return an instance of Pack, holding all the data of this class.
     */
    public Pack toPack() {
        Pack pack = new Pack();
        pack.putString(USER_TITLE, getTitle());
        pack.putString(USER_CONTENT, getContent());
        pack.putString(USER_BIG_TITLE, getBigTitle());
        pack.putString(USER_BIG_CONTENT, getBigContent());
        pack.putString(USER_SUMMARY, getSummary());
        pack.putString(USER_IMG_URL, getImageUrl());
        pack.putString(USER_ICON_URL, getIconUrl());
        pack.putString(USER_JSON, customContent);
        ListPack list = new ListPack();
        for (NotificationButtonData b : getButtons()) {
            list.addPack(b.toPack());
        }
        pack.putListPack(USER_BUTTONS, list);
        return pack;
    }

    /**
     * In order to get the data from a pack data without needing to parse manually.
     * @param pack is the data holder.
     * @return an instance of this class made from that pack.
     */
    public static NotificationData fromPack(Pack pack) {
        // Get the buttons from list pack
        ListPack btnList = pack.getListPack(USER_BUTTONS);
        List<NotificationButtonData> userNotifButtons = new ArrayList<>();
        if (btnList != null) {
            for (Object o : btnList) {
                if (o instanceof Pack) {
                    Pack i = (Pack) o;
                    userNotifButtons.add(NotificationButtonData.fromPack(i));
                }
            }
        }
        return new NotificationData(
                pack.getString(USER_TITLE),
                pack.getString(USER_CONTENT),
                pack.getString(USER_BIG_TITLE),
                pack.getString(USER_BIG_CONTENT),
                pack.getString(USER_SUMMARY),
                pack.getString(USER_IMG_URL),
                pack.getString(USER_ICON_URL),
                pack.getString(USER_JSON),
                userNotifButtons
        );
    }

}
