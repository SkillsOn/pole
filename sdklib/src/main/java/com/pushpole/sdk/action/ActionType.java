package com.pushpole.sdk.action;

import java.util.HashMap;
import java.util.Map;

import com.pushpole.sdk.action.actions.AppAction;
import com.pushpole.sdk.action.actions.CafeBazaarRateAction;
import com.pushpole.sdk.action.actions.DialogAction;
import com.pushpole.sdk.action.actions.DismissAction;
import com.pushpole.sdk.action.actions.DownloadAndWebviewAction;
import com.pushpole.sdk.action.actions.DownloadFileAction;
import com.pushpole.sdk.action.actions.IntentAction;
import com.pushpole.sdk.action.actions.UserActivityAction;
import com.pushpole.sdk.action.actions.WebViewAction;
import com.pushpole.sdk.action.actions.UrlAction;

/**
 * A type of an {@link Action} which links to the {@link ActionFactory} for creating the
 * {@code Action}.
 * <p>
 * Each {@code ActionType} uniquely defines an {@code Action} class along with it's code, and
 * Factory class.
 *
 * @author Hadi Zolfaghari
 */
public enum ActionType {

    /**
     * Dismiss Action; does nothing special.
     */
    DISMISS("D", false, new DismissAction.Factory()),

    /**
     * Open Application Action; opens the application.
     */
    APP("A", false, new AppAction.Factory()),

    /**
     * Open URL Action; opens a specific url.
     */
    URL("U", false, new UrlAction.Factory()),

    /**
     * Open Dialog Action; shows the notification in a dialog.
     * action dialog was 'P' in versions<0.10.0, changed to G from 0.10.0 onward
     */
    DIALOG("G", true, new DialogAction.Factory()),

    /**
     * Intent Action; fires a custom intent.
     */
    INTENT("I", true, new IntentAction.Factory()),

    /**
     * Cafe Bazaar Rate Action; Opens Cafe Bazaar rating activity for the application.
     */
    CAFE_BAZAAR_RATE("C", true, new CafeBazaarRateAction.Factory()),

    /**
     * Shows the specified url in a webview
     */
    SHOW_IN_WEBVIEW("W", true, new WebViewAction.Factory()),

   /**
    * Downloads apk file from given url and lunches its installation
    */
    DOWNLOAD("L", false, new DownloadFileAction.Factory()),

    /**
     * Show a url in webview and downloads apk file from another given url and lunches its installation
     */
    DOWNLOAD_AND_WEBVIEW("O", false, new DownloadAndWebviewAction.Factory()),

    /**
     * Starts the activity which its name is given by user
     */
    USER_ACTIVITY("T", false, new UserActivityAction.Factory());

    private ActionFactory mFactory;
    private String mCode;
    private boolean mRequireMessageData;
    private static Map<String, ActionType> mActionMap;


    /**
     * Constructor for an {@code ActionType}
     *
     * @param code               the unique code to identify this {@code ActionType} with.
     *                           <p>
     *                           This code is used in serializing this {@code ActionType} and communicating
     *                           with the server.
     * @param requireMessageData determines whether the notification message data should be included
     *                           in the data given the {@link ActionFactory} when creating an
     *                           {@link Action} with this {@code ActionType}.
     * @param factory            the {@link ActionFactory} which creates instances of the {@link Action} with
     *                           this {@code ActionType}.
     */
    ActionType(String code, boolean requireMessageData, ActionFactory factory) {
        mCode = code;
        mRequireMessageData = requireMessageData;
        mFactory = factory;
    }

    public String getCode() {
        return mCode;
    }

    public ActionFactory getFactory() {
        return mFactory;
    }

    public boolean requiresMessageData() {
        return mRequireMessageData;
    }

    /**
     * Gets an {@code ActionType} from its code String.
     *
     * @param code the {@code ActionType} code String.
     * @return the {@code ActionType} relating to the given code.
     */
    public static ActionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return mActionMap.get(code);
    }


    static {
        mActionMap = new HashMap<>();
        for (ActionType actionType : ActionType.values()) {
            mActionMap.put(actionType.getCode(), actionType);
        }
    }

}
