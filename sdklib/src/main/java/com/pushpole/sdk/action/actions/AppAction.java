package com.pushpole.sdk.action.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.pushpole.sdk.action.Action;
import com.pushpole.sdk.action.ActionFactory;
import com.pushpole.sdk.action.ActionType;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.util.Pack;

/***
 * Open Application Action
 */
public class AppAction extends Action {
    @Override
    public void execute(Context context) {
        Logger.info("[Notif-Action] Running Application");
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        context.startActivity(intent);
    }

    /**
     * return action type
     *
     * @return {@code ActionType.APP}
     */
    @Override
    public ActionType getActionType() {
        return ActionType.APP;
    }

    /**
     * factory class to generate {@link AppAction} instance
     */
    public static class Factory extends ActionFactory {
        /**
         * generate {@link AppAction} instance
         *
         * @param data the data to create the {@code Action} with given as a {@link Pack}
         * @return
         */
        @Override
        public Action buildAction(Pack data) {
            return new AppAction();
        }
    }
}
