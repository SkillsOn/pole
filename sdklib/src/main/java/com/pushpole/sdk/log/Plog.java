package com.pushpole.sdk.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Plog {
    private static Plog instance;

    public static Plog getInstance() {
        if (instance == null) {
            instance = new Plog();
        }
        return instance;
    }

    private Plog() {
    }

    private List<LogHandler> logHandlers = new ArrayList<>();

    void addHandler(LogHandler handler) {
        logHandlers.add(handler);
    }

    void removeAllHandlers() {
        logHandlers.clear();
    }

    private void broadcastLog(LogItem logItem) {
        for (LogHandler logHandler : logHandlers) {
            logHandler.onLog(logItem);
        }
    }

    void log(LogItem logItem) {
        broadcastLog(logItem);
    }

    private static void log(LogLevel level, String message, Throwable t) {
        getInstance().log(new LogItem(level, message, t));
    }

    private static void log(LogLevel level, String message) {
        getInstance().log(new LogItem(level, message, null, null));
    }

    void trace(String message) {
        log(LogLevel.TRACE, message);
    }

    void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    void info(String message) {
        log(LogLevel.INFO, message);
    }

    void warn(String message) {
        log(LogLevel.WARN, message);
    }

    void warn(String message, Throwable t) {
        log(LogLevel.INFO, message, t);
    }

    void error(String message) {
        log(LogLevel.ERROR, message);
    }

    void error(String message, Throwable t) {
        log(LogLevel.ERROR, message, t);
    }

    void error(Throwable t) {
        log(LogLevel.ERROR, null, t);
    }

    void wtf(String message) {
        log(LogLevel.WTF, message);
    }

    void wtf(String message, Throwable t) {
        log(LogLevel.WTF, message, t);
    }

    void wtf(Throwable t) {
        log(LogLevel.WTF, null, t);
    }

    LogItem buildTrace() {
        return new LogItem(LogLevel.TRACE, null, null, null);
    }

    LogItem buildDebug() {
        return new LogItem(LogLevel.DEBUG, null, null, null);
    }

    LogItem buildInfo() {
        return new LogItem(LogLevel.INFO, null, null, null);
    }

    LogItem buildWarn() {
        return new LogItem(LogLevel.WARN, null, null, null);
    }

    LogItem buildError() {
        return new LogItem(LogLevel.ERROR, null, null, null);
    }

    LogItem buildWtf() {
        return new LogItem(LogLevel.WTF, null, null, null);
    }
}

class LogItem {
    public LogItem(LogLevel level, String message, Throwable t) {
        this(level, message, t, null);
    }

    public LogItem(LogLevel level, String message, Map<String, Object> logData) {
        this(level, message, null, logData);
    }

    public LogItem(LogLevel level, String message, Throwable t, Map<String, Object> logData) {
        this.level = level;
        this.message = message;
        this.throwable = t;
        this.logData = logData;
        this.timestamp = Calendar.getInstance().getTime();
    }

    String message;
    Set<String> tags;
    LogLevel level;
    Throwable throwable;
    LogLevel logCatLevel;
    Map<String, Object> logData;
    Date timestamp;
    Boolean isBreadcrumb = false;
    Boolean forceReport = false;
    String culprit;

    LogItem withData(String key, Object value) {
        if (logData == null) {
            logData = new HashMap<>();
        }
        logData.put(key, value);
        return this;
    }

    LogItem message(String value)  {
        message = value;
        return this;
    }

    LogItem withError(Throwable value) {
        throwable = value;
        return this;
    }

    LogItem withTag(String ...values)  {
        tags.addAll(Arrays.asList(values));
        return this;
    }

    LogItem withBreadcrumb()  {
        isBreadcrumb = true;
        return this;
    }

    LogItem reportToSentry()  {
        forceReport = true;
        return this;
    }

    LogItem useLogCatLevel(LogLevel logLevel)  {
        logCatLevel = logLevel;
        return this;
    }

    LogItem culprit(String value)  {
        culprit = value;
        return this;
    }

    void log() {
        Plog.getInstance().log(this);
    }
}


enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    WTF
}
