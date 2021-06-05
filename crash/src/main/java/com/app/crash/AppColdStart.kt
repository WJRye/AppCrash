package com.app.crash


import android.app.Activity
import android.app.Application

/**
 * 检测应用程序是冷启动还是热启动
 */
internal class AppColdStart private constructor() {

    companion object {
        /**
         * 记录启动的Activity
         */
        private var mActivityCount: Int = -1

        /**
         * 是否是冷启动
         */
        private var mIsColdStart: Boolean = true

        /**
         * 检测是冷启动还是热启动
         */
        fun detectColdStart(application: Application) {
            application.registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksAdapterImpl())
        }

        /**
         * 是否是冷启动
         */
        fun isColdStart(): Boolean {
            return mIsColdStart
        }
    }


    class ActivityLifecycleCallbacksAdapterImpl : ActivityLifecycleCallbacksAdapter() {

        override fun onActivityStarted(activity: Activity?) {
            super.onActivityStarted(activity)
            if (mActivityCount == 0) {
                mIsColdStart = false
            } else if (mActivityCount == -1) {
                mActivityCount = 0
            }
            mActivityCount++
        }

        override fun onActivityStopped(activity: Activity?) {
            super.onActivityStopped(activity)
            mActivityCount--
        }

    }
}