package com.example.app.crash;

import android.app.Application;

import com.app.crash.CrashHandler;

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.INSTANCE.init(this);
    }
}
