package com.pushpole.sdk.action.actions;

import android.content.Context;

import com.pushpole.sdk.action.Action;
import com.pushpole.sdk.action.ActionFactory;
import com.pushpole.sdk.action.ActionType;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.util.Pack;

/**
 * Dismiss Action; does nothing special.
 */
public class DismissAction extends Action {
    @Override
    public void execute(Context context) {
        Logger.info("Executing dismiss action...");
        // Do Nothing
    }

    @Override
    public ActionType getActionType() {
        return ActionType.DISMISS;
    }

    public static class Factory extends ActionFactory {

        @Override
        public Action buildAction(Pack data) {
            return new DismissAction();
        }
    }
}
