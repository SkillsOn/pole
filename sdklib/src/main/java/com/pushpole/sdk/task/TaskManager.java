package com.pushpole.sdk.task;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.HashMap;
import java.util.Map;

import com.pushpole.sdk.Constants;
import com.pushpole.sdk.internal.db.KeyStore;
import com.pushpole.sdk.internal.log.LogData;
import com.pushpole.sdk.internal.log.Logger;
import com.pushpole.sdk.service.IntentTaskRunner;
import com.pushpole.sdk.task.options.DefaultOptions;
import com.pushpole.sdk.task.options.Defaults;
import com.pushpole.sdk.task.options.SingletonTask;
import com.pushpole.sdk.task.options.TaskOptions;
import com.pushpole.sdk.task.scheduler.Scheduler;
import com.pushpole.sdk.task.scheduler.evernote.EvernoteScheduler;
import com.pushpole.sdk.task.tasks.NotificationHandleTask;
import com.pushpole.sdk.util.IdGenerator;
import com.pushpole.sdk.util.InvalidJsonException;
import com.pushpole.sdk.util.Pack;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/***
 * A singleton class for scheduling tasks
 * task will be scheduled to run with {GcmNetworkManager} or {@link IntentTaskRunner}
 * if GcmNetworkManager not available then task will be scheduled with {FallbackGcmNetworkManager}
 */
public class TaskManager {
    private volatile static TaskManager mInstance;

    private Map<String, PushPoleAsyncTask> mAsyncTaskStore;


    private Scheduler mScheduler;
    private Context mContext;

    private Handler mHandler;

    /***
     * private constructor
     * initialize {@code mAsyncTaskStore}
     */
    private TaskManager(Context context) {
        mAsyncTaskStore = new HashMap<>();
        mScheduler = new EvernoteScheduler(context);

        HandlerThread handlerThread = new HandlerThread("pushpole-background", THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    /***
     * return single instance of {@link TaskManager}, always use this method for creating instance of {@link TaskManager}
     *
     * @return
     */
    public static TaskManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (TaskManager.class) {
                if (mInstance == null) {
                    mInstance = new TaskManager(context);
                }
            }
        }
        mInstance.setContext(context);
        return mInstance;
    }

    private void setContext(Context context) {
        mContext = context;
    }

    private Scheduler getScheduler() {
        return mScheduler;
    }

    public Intent createTaskIntent(Class<? extends PushPoleTask> taskType, Pack taskData) {
        Intent intent = new Intent(mContext, IntentTaskRunner.class);
        intent.setAction(Constants.getVal(Constants.ACTION_RUN_TASK));
        if (taskType != null) {
            intent.putExtra(Constants.getVal(Constants.TASK_TYPE), taskType.getName());
        }
        if (taskData != null) {
            intent.putExtra(Constants.getVal(Constants.TASK_DATA), taskData.toJson());
        }
        return intent;
    }

    /**
     * Start schedule {@link PushPoleAsyncTask} to run on {@link IntentTaskRunner}
     * the task will put on the {@link IntentTaskRunner} queue if {@link IntentTaskRunner} is busy
     */
    public void asyncTask(final PushPoleAsyncTask task, final Pack taskData) {
        // TODO: Obsolete way of handling tasks, should be removed
//        Intent intent = createTaskIntent(null, taskData);
//        intent.putExtra(Constants.getVal(Constants.TASK_ID), task.getId());
//        mAsyncTaskStore.put(task.getId(), task);
//        mContext.startService(intent);
        try {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.runTask(mContext, taskData);
                    } catch (Exception e) {
                        Logger.error("Error occurred while running async task", e);
                        android.util.Log.e("PushPole", "Failed to run PushPole task", e);
                    }
                }
            });
        } catch (Exception e) {
            Logger.error("Error occurred while running task on async thread", e);
            android.util.Log.e("PushPole", "Failed to run PushPole task on async thread", e);
        }
    }

    public void asyncTaskDelayed(final PushPoleAsyncTask task, Long millis) {
        try {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.runTask(mContext, null);
                    } catch (Exception e) {
                        Logger.error("Error occurred while running async task", e);
                        android.util.Log.e("PushPole", "Failed to run PushPole task", e);
                    }
                }
            }, millis);
        } catch (Exception e) {
            Logger.error("Error occurred while running task on async thread", e);
            android.util.Log.e("PushPole", "Failed to run PushPole task on async thread", e);
        }
    }


    /***
     * schedule {@link PushPoleAsyncTask} with {@code null} data
     *
     * @param task the task
     */
    public void asyncTask(PushPoleAsyncTask task) {
        asyncTask(task, null);
    }


    /***
     * get async task by ID
     *
     * @param id the unique id
     * @return
     */
    public PushPoleAsyncTask getAsyncTask(String id) {
        return mAsyncTaskStore.get(id);
    }

    public void scheduleTask(Class<? extends PushPoleTask> taskType) {
        scheduleTask(taskType, null, null);
    }

    public void scheduleTask(Class<? extends PushPoleTask> taskType, TaskOptions taskOptions) {
        scheduleTask(taskType, null, taskOptions);
    }

    public void scheduleTask(Class<? extends PushPoleTask> taskType, Pack taskData) {
        scheduleTask(taskType, taskData, null);
    }

    public void scheduleTask(Class<? extends PushPoleTask> taskType, Pack taskData, TaskOptions taskOptions) {
        /* Set default task options */
        TaskOptions.Builder optionsBuilder = new TaskOptions.Builder();
        if (taskOptions != null) {
            optionsBuilder.update(taskOptions);
        }
        if (isTaskSingleton(taskType)) {
            optionsBuilder.setReplace(true);
        }
        DefaultOptions defaultOptions = taskType.getAnnotation(DefaultOptions.class);
        if (defaultOptions != null) {
            optionsBuilder.setDefaults(defaultOptions);
        } else {
            optionsBuilder.setDefaults(new Defaults());
        }
        taskOptions = optionsBuilder.build();

        String tag = null;
        String dataKey = null;
        if (isTaskSingleton(taskType)) {
            dataKey = taskType.getSimpleName();
        } else if (taskData != null) {
            dataKey = IdGenerator.generateUUID(4);
        }

        if (taskData != null) {
            KeyStore.getInstance(mContext).putPack(dataKey, taskData);
        }

        tag = getTaskInstanceTag(taskType, dataKey);

        getScheduler().schedule(mContext, tag, taskOptions);

        Logger.debug("Scheduling GCM Task", new LogData(
                "Type", taskType.toString(),
                "Tag", tag
        ));
    }

    public void cancelTask(Class<? extends PushPoleTask> taskType) {
        if (!isTaskSingleton(taskType)) {
            throw new IllegalArgumentException("Cannot cancel non-singleton task");
        }

        String tag = getTaskInstanceTag(taskType, taskType.getSimpleName());
        getScheduler().cancel(mContext, tag);
    }


    /***
     * a handler method for tasks that started with {@code PushPole.ACTION_RUN_TASK} intent
     * retrieving task, task type and task data
     * and run task
     *
     * @param intent
     */
    public void handleAsyncTask(Intent intent) {

        String taskTypeStr = intent.getStringExtra(Constants.getVal(Constants.TASK_TYPE));

        String taskId = intent.getStringExtra(Constants.getVal(Constants.TASK_ID));
        PushPoleTask task = null;
        if (taskId != null) {
            task = getAsyncTask(taskId);
            // Error if task is null
            if (task == null) {
                return;
            }
        } else if (taskTypeStr != null && taskTypeStr.contains(NotificationHandleTask.class.getName())) {
            task = new NotificationHandleTask(); //to handle click/dismiss actions on notifications
        }

        // Get Task Data from intent
        String jsonStr = intent.getStringExtra(Constants.getVal(Constants.TASK_DATA));
        Pack taskData = null;
        if (jsonStr != null) {
            try {
                taskData = Pack.fromJson(jsonStr);
            } catch (InvalidJsonException e) {
                Logger.error("Error parsing task json data from intent", e);
            }
        }
        try {
            task.runTask(mContext, taskData);
        } catch (Throwable t) {
            Logger.error("Error in running a task", t);
            t.printStackTrace();
        }

    }


    /***
     * a handler method for tasks that scheduled by GcmNetworkManager
     * retrieving task, task type and task data from {@code taskTag}
     * and run task
     *
     * @param taskTag the task tag
     * @return result
     */
    public Result handleScheduledTask(String taskTag) {
        Logger.debug("Running GCM Task", new LogData(
                "Tag", taskTag
        ));

        Class<? extends PushPoleTask> taskType = getTaskTypeFromInstanceTag(taskTag);
        String dataKey = getTaskDataKeyFromTag(/*context,*/ taskTag);
        Pack taskData = dataKey == null ? null : KeyStore.getInstance(mContext).getPack(dataKey, null);

        if (taskType == null) {
            Logger.warning("Invalid task type %s", taskTag);
            return Result.FAIL;
        }

        PushPoleTask task = null;
        try {
            task = taskType.newInstance();
        } catch (InstantiationException e) {
            Logger.error("Creating task instance %s failed", taskTag, e);
            return Result.FAIL;
        } catch (IllegalAccessException e) {
            Logger.error("Creating task instance %s failed", taskTag, e);
            return Result.FAIL;
        }


        int retryCount = -1;

        if (taskData != null) {
            retryCount = getAndIncRetryCount(taskData);
        }

        Result result = task.runTask(mContext, taskData);

        if (taskData != null && result != Result.RESCHEDULE) {
            KeyStore.getInstance(mContext).delete(dataKey);
        } else if (taskData != null) {
            KeyStore.getInstance(mContext).putPack(dataKey, taskData);
        }

        if (result == Result.RESCHEDULE && retryCount >= 0) {
            DefaultOptions taskOptions = taskType.getAnnotation(DefaultOptions.class);
            if (taskOptions != null && taskOptions.retryCount() > 0 && retryCount > taskOptions.retryCount()) {
                Logger.warning("Task " + taskType + " failed too many times, aborting");
                if(taskType.getName().contains("NotificationBuildTask")){
                    NotificationBuildTask.sendNotifPublishStatus(mContext,
                            taskData.getString(Constants.getVal(Constants.MESSAGE_ID), null), NotificationBuildTask.NOTIF_FAILED);
                }
                return Result.FAIL;
            } else {
                Logger.warning("Task " + taskType + " attempt %d failed", retryCount);
            }
        }

        return result;
    }

    /***
     * get and increment retry count by 1
     *
     * @param taskPack the test pack
     * @return retry count
     */
    private int getAndIncRetryCount(Pack taskPack) {
        String retryCountStr = taskPack.getString(Constants.getVal(Constants.RETRY_COUNT), "0");
        int retryCount;
        try {
            retryCount = Integer.parseInt(retryCountStr);
        } catch (NumberFormatException e) {
            retryCount = 0;
        }
        retryCount += 1;
        taskPack.putString(Constants.getVal(Constants.RETRY_COUNT), String.valueOf(retryCount));
        return retryCount;
    }

    /***
     * return task tag
     *
     * @param taskType the task type
     * @param dataKey  the data key
     * @return the tag
     */
    private String getTaskInstanceTag(Class<? extends PushPoleTask> taskType, String dataKey) {
        String type = taskType.getName();
        if (dataKey == null) {
            return type;
        }
        return type + "#" + dataKey;
    }

    /***
     * return task type by tag
     *
     * @param tag the tag
     * @return the task type or {@code null}
     */
    private Class<? extends PushPoleTask> getTaskTypeFromInstanceTag(String tag) {
        String type = null;
        try {
            if (tag.contains("#")) {
                String[] parts = tag.split("#");
                type = parts[0];
            } else {
                type = tag;
            }
            return getClass().getClassLoader().loadClass(type).asSubclass(PushPoleTask.class);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (ClassNotFoundException e) {
            Logger.warning("Task " + type + " not found");
            return null;
        }
    }

    /***
     * return task data key by tag
     *
     * @param tag the tag
     * @return the key or {@code null}
     */
    private String getTaskDataKeyFromTag(/*Context context,*/ String tag) {
        if (tag.contains("#")) {
            String[] parts = tag.split("#");
            return parts[1];
        }
        return null;
    }

    private boolean isTaskSingleton(Class<? extends PushPoleTask> taskType) {
        return taskType.getAnnotation(SingletonTask.class) != null;
    }

}
