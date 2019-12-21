package com.pushpole.sdk.task.scheduler;

import android.content.Context;

import com.pushpole.sdk.task.options.TaskOptions;


/**
 * Created by hadi on 7/19/16.
 */
public interface Scheduler {
    void schedule(Context context, String tag, TaskOptions taskOptions);
    void cancel(Context context, String tag);
}
