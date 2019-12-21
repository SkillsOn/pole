package com.pushpole.sdk.collection;

import com.pushpole.sdk.Constants;
import com.pushpole.sdk.collection.tasks.AppListTask;
import com.pushpole.sdk.collection.tasks.CheckIsHiddenAppTask;
import com.pushpole.sdk.collection.tasks.DetectUserActivityTask;
import com.pushpole.sdk.collection.tasks.MobileCellTask;
import com.pushpole.sdk.collection.tasks.ConstantTask;
import com.pushpole.sdk.collection.tasks.FloatingTask;
import com.pushpole.sdk.collection.tasks.VariableTask;
import com.pushpole.sdk.collection.tasks.WifiTask;
import com.pushpole.sdk.task.PushPoleTask;

/**
 * Created by hadi on 7/20/16.
 */
public enum CollectionType {
    FLOATING(FloatingTask.class, Constants.getVal(Constants.FLOATING_DATA_T)),
    VARIABLE(VariableTask.class, Constants.getVal(Constants.VARIABLE_DATA_T)),
    CONSTANT(ConstantTask.class, Constants.getVal(Constants.CONSTANT_DATA_T)),
    APP_LIST(AppListTask.class, Constants.getVal(Constants.APP_LIST_T)),
    WIFI_LIST(WifiTask.class, Constants.getVal(Constants.WIFI_LIST_T)),
    CELL_INFO(MobileCellTask.class, Constants.getVal(Constants.CELL_INFO_T)),
    DETECTED_ACTIVITY(DetectUserActivityTask.class, Constants.getVal(Constants.DETECTED_ACTIVITY_T)),
    CHECK_HIDDEN_APP(CheckIsHiddenAppTask.class, Constants.getVal(Constants.CHECK_IS_HIDDEN_APP_T));

    private Class<? extends PushPoleTask> mCollectionTask;
    private String mCode;

    private CollectionType(Class<? extends PushPoleTask> collectionTask, String code) {
        mCollectionTask = collectionTask;
        mCode = code;
    }

    public static CollectionType fromCode(String code) {
        for (CollectionType cType : CollectionType.values()) {
            if (cType.getCode().equals(code)) {
                return cType;
            }
        }
        return null;
    }

    public Class<? extends PushPoleTask> getCollectionTask() {
        return mCollectionTask;
    }

    public String getCode() {
        return mCode;
    }
}
