package com.app.crash;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.system.Os;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 收集信息
 */
public final class CrashInfo {


    private static final String TAG = "CrashInfo";

    private static boolean DEBUG = false;

    private CrashInfo() {

    }


    /**
     * 跟踪应用程序
     *
     * @param application Application
     */
    public static void track(Application application) {
        setDebug(application);
        AppColdStart.Companion.detectColdStart(application);
        TrackActivity.track(application, DEBUG);
    }

    /**
     * 跟踪用户
     *
     * @return
     */
    private static String getTrackActivityPath() {
        return TrackActivity.getTrackActivityPath();
    }

    /**
     * 跟踪用户
     *
     * @return
     */
    private static String getTrackActivityPathDetail() {
        return TrackActivity.getTrackActivityPathDetail();
    }

    private static boolean isColdStart() {
        return AppColdStart.Companion.isColdStart();
    }


    /**
     * 设置调试状态
     *
     * @param context 上下文对象
     */
    private static void setDebug(Application context) {
        DEBUG = context.getApplicationInfo() != null &&
                (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }


    /**
     * 获得所有消息
     *
     * @param context 上下文对象
     * @return LinkedHashMap
     */
    public static Map<String, String> getAll(Application context) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("DisplayMetrics", getDisplayMetrics(context));
        map.put("AppMemory", getAppMemory(context));
        map.put("DebugAppMemory", getDebugAppMemory(context));
        map.put("DeviceSystemMemory", getDeviceSystemMemory(context));
        map.put("DeviceSDCardMemory", getDeviceSDCardMemory(context));
        map.put("Activities", getActivities(context, false));
        map.put("ActivitiesWithFragments", getActivities(context, true));
        map.put("isColdStart", String.valueOf(isColdStart()));
        map.put("TrackActivityPath", getTrackActivityPath());
        map.put("TrackActivityPathDetail", getTrackActivityPathDetail());
//        map.put("FrescoInfo", getFrescoInfo(context));
        map.put("ThreadInfo", getThreadInfo());
        map.put("FdInfo", getFdInfo());
        map.put("GCInfo", getGCInfo());
        if (DEBUG) {
//            dumpToDefaultFile(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Log.d(TAG, JSONObject.wrap(map).toString());
            }
        }

        return map;
    }


    /**
     * 设备屏幕信息
     *
     * @param context 上下文对象
     * @return
     */
    private static String getDisplayMetrics(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        JSONObject dmJSON = new JSONObject();
        try {
            dmJSON.put("brand", Build.BRAND);
            dmJSON.put("release", Build.VERSION.RELEASE);
            dmJSON.put("model", Build.MODEL);
            dmJSON.put("manufacturer", Build.MANUFACTURER);
            dmJSON.put("sdk", Build.VERSION.SDK_INT);
            dmJSON.put("width*height", new StringBuilder().append(dm.widthPixels).append('*').append(dm.heightPixels).toString());
            dmJSON.put("densityDpi", String.valueOf(dm.densityDpi));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dmJSON.toString();
    }


    /**
     * 程序运行时内存信息
     *
     * @param context 上下文对象
     * @return
     */
    private static String getAppMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        JSONObject appMemoryJSON = new JSONObject();
        try {
            if (am != null) {
                appMemoryJSON.put("memoryClass", BytesUtil.formatFileSizeByMB(context, am.getMemoryClass()));
                appMemoryJSON.put("largeMemoryClass", BytesUtil.formatFileSizeByMB(context, am.getLargeMemoryClass()));
            }
            Runtime r = Runtime.getRuntime();
            appMemoryJSON.put("maxMemory", BytesUtil.formatFileSizeByBytes(context, r.maxMemory()));//最大可用内存
            appMemoryJSON.put("totalMemory", BytesUtil.formatFileSizeByBytes(context, r.totalMemory()));//当前可用内存
            appMemoryJSON.put("freeMemory", BytesUtil.formatFileSizeByBytes(context, r.freeMemory()));//当前空闲内存
            appMemoryJSON.put("usedMemory", BytesUtil.formatFileSizeByBytes(context, r.totalMemory() - r.freeMemory()));//当前已使用内存
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appMemoryJSON.toString();
    }


    /**
     * 调试下程序运行时内存信息
     *
     * @param context 上下文对象
     * @return
     */
    @TargetApi(value = Build.VERSION_CODES.M)
    private static String getDebugAppMemory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(debugMemoryInfo);
            Map<String, String> map = debugMemoryInfo.getMemoryStats();
            Set<Map.Entry<String, String>> set = map.entrySet();
            Iterator<Map.Entry<String, String>> iterator = set.iterator();
            JSONObject jsonObject = new JSONObject();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String value = BytesUtil.formatFileSizeByKB(context, Long.parseLong(entry.getValue()));
                try {
                    jsonObject.put(entry.getKey(), value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return jsonObject.toString();
        }

        return null;
    }


    /**
     * 系统内存信息
     *
     * @param context 上下文对象
     * @return
     */
    private static String getDeviceSystemMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        JSONObject deviceMemoryJSON = new JSONObject();
        if (am != null) {
            am.getMemoryInfo(memoryInfo);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    deviceMemoryJSON.put("totalMem", BytesUtil.formatFileSizeByBytes(context, memoryInfo.totalMem));//系统总内存
                }
                deviceMemoryJSON.put("availMem", BytesUtil.formatFileSizeByBytes(context, memoryInfo.availMem));//系统剩余内存
                deviceMemoryJSON.put("threshold", BytesUtil.formatFileSizeByBytes(context, memoryInfo.threshold));//当系统剩余内存低于"+threshold+"时就看成低内存运行
                deviceMemoryJSON.put("lowMemory", memoryInfo.lowMemory);//系统是否处于低内存运行
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return deviceMemoryJSON.toString();

    }

    /**
     * 设备SD卡内存
     *
     * @param context 上下文对象
     * @return
     */
    private static String getDeviceSDCardMemory(Context context) {
        JSONObject deviceMemoryJSON = new JSONObject();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                deviceMemoryJSON.put("sdCardTotalSpace", BytesUtil.formatFileSizeByBytes(context, Environment.getExternalStorageDirectory().getTotalSpace()));//sd card总内存
//                deviceMemoryJSON.put("sdCardFreeSpace", Formatter.formatFileSize(context, Environment.getExternalStorageDirectory().getFreeSpace()));//sd card剩余内存
                deviceMemoryJSON.put("sdCardUsableSpace", BytesUtil.formatFileSizeByBytes(context, Environment.getExternalStorageDirectory().getUsableSpace()));//sd card可用内存
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deviceMemoryJSON.toString();
    }


    /**
     * 获取内存快照，并存储文件到默认路径
     *
     * @param context 上下文对象
     */
    private static void dumpToDefaultFile(Context context) {
        dumpToFile(Environment.getExternalStorageDirectory().getPath() + File.separator + context.getPackageName() + File.separator + "dump");
    }

    /**
     * 获取内存快照
     *
     * @param pathname 存储文件路径
     */
    private static void dumpToFile(String pathname) {

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return;
        }

        try {
            File file = new File(pathname);
            if (!file.exists()) {
                file.mkdirs();
            }
            Calendar calendar = SimpleDateFormat.getDateTimeInstance().getCalendar();
            calendar.setTime(new Date(System.currentTimeMillis()));
            calendar.get(Calendar.YEAR);
            StringBuilder sb = new StringBuilder();
            int[] fields = {Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};
            char splitChar = '-';
            for (int field : fields) {
                sb.append(calendar.get(field)).append(splitChar);
            }
            if (sb.length() > 1) {
                sb.deleteCharAt(sb.length() - 1).append(".hprof");
            }
            String name = sb.toString();
            if (DEBUG)
                Log.d(TAG, "dumpToFile-->filename=" + name);
            Debug.dumpHprofData(new File(file, name).getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Activity栈信息
     *
     * @param application     应用程序对象
     * @param includeFragment true包括Fragment，否则不包括
     * @return
     */
    private static String getActivities(Application application, boolean includeFragment) {
        List<Activity> list = getActivitiesByApplication(application);
        if (list == null || list.isEmpty()) return null;

        Map<String, List<String>> stackMap = null;
        List<String> stackList = null;
        if (includeFragment) {
            stackMap = new LinkedHashMap<>(list.size());
        } else {
            stackList = new ArrayList<>(list.size());
        }
        Iterator<Activity> iterator = list.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (includeFragment) {
                Map<String, List<String>> temp = null;
                if (activity instanceof FragmentActivity) {
                    temp = getSupportFragmentOfActivity(activity.getClass().getName(), ((FragmentActivity) activity).getSupportFragmentManager());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    temp = getFragmentOfActivity(activity.getClass().getName(), activity.getFragmentManager());
                }
                if (temp != null && !temp.isEmpty()) stackMap.putAll(temp);
            } else {
                stackList.add(activity.getClass().getName());
            }
        }
        if (includeFragment) {
            return new JSONObject(stackMap).toString();
        }

        return new JSONArray(stackList).toString();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static Map<String, List<String>> getFragmentOfActivity(String
                                                                           name, android.app.FragmentManager fragmentManager) {
        Map<String, android.app.FragmentManager> fragmentManagers = new LinkedHashMap<>();
        fragmentManagers.put(name, fragmentManager);
        Map<String, android.app.FragmentManager> childFragmentManagers = null;
        Map<String, List<String>> result = new LinkedHashMap<>();
        int size = 4;
        while (!fragmentManagers.isEmpty()) {
            childFragmentManagers = new LinkedHashMap<>(size);
            Set<Map.Entry<String, android.app.FragmentManager>> entries = fragmentManagers.entrySet();
            Iterator<Map.Entry<String, android.app.FragmentManager>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, android.app.FragmentManager> entry = iterator.next();
                android.app.FragmentManager fm = entry.getValue();
                List<android.app.Fragment> fragments = fm.getFragments();
                List<String> values = new ArrayList<>(fragments.size());
                int index = 0;
                for (android.app.Fragment fragment : fragments) {
                    String fragmentName = fragment.getClass().getName();
                    if (!fragment.getChildFragmentManager().getFragments().isEmpty()) {
                        childFragmentManagers.put(values.contains(fragmentName) ? fragmentName + index : fragmentName, fragment.getChildFragmentManager());
                    }
                    values.add(fragmentName);
                    index++;
                }
                result.put(entry.getKey(), values);
            }

            fragmentManagers.clear();
            if (!childFragmentManagers.isEmpty())
                fragmentManagers.putAll(childFragmentManagers);
        }

        return result;

    }


    private static Map<String, List<String>> getSupportFragmentOfActivity(String
                                                                                  name, FragmentManager fragmentManager) {
        Map<String, FragmentManager> fragmentManagers = new LinkedHashMap<>();
        fragmentManagers.put(name, fragmentManager);
        Map<String, List<String>> result = new LinkedHashMap<>();
        Map<String, FragmentManager> childFragmentManagers = null;
        int size = 4;
        while (!fragmentManagers.isEmpty()) {
            childFragmentManagers = new LinkedHashMap<>(size);
            Set<Map.Entry<String, FragmentManager>> entries = fragmentManagers.entrySet();
            Iterator<Map.Entry<String, FragmentManager>> iterator = entries.iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, FragmentManager> entry = iterator.next();
                FragmentManager fm = entry.getValue();
                List<Fragment> fragments = fm.getFragments();
                List<String> values = new ArrayList<>(fragments.size());
                int index = 0;
                for (Fragment fragment : fragments) {
                    String fragmentName = fragment.getClass().getName();
                    if (!fragment.getChildFragmentManager().getFragments().isEmpty()) {
                        childFragmentManagers.put(values.contains(fragmentName) ? fragmentName + index : fragmentName, fragment.getChildFragmentManager());
                    }
                    values.add(fragmentName);
                    index++;
                }
                result.put(entry.getKey(), values);
            }

            fragmentManagers.clear();
            if (!childFragmentManagers.isEmpty())
                fragmentManagers.putAll(childFragmentManagers);
        }

        return result;

    }


    /**
     * 通过反射获取应用程序的Activity栈信息
     *
     * @param application 应用程序对象
     * @return Activity栈列表
     */
    private static List<Activity> getActivitiesByApplication(Application application) {
        List<Activity> list = new ArrayList<>();
        try {
            Class<Application> applicationClass = Application.class;
            Field mLoadedApkField = applicationClass.getDeclaredField("mLoadedApk");
            mLoadedApkField.setAccessible(true);
            Object mLoadedApk = mLoadedApkField.get(application);
            Class<?> mLoadedApkClass = mLoadedApk.getClass();
            Field mActivityThreadField = mLoadedApkClass.getDeclaredField("mActivityThread");
            mActivityThreadField.setAccessible(true);
            Object mActivityThread = mActivityThreadField.get(mLoadedApk);
            Class<?> mActivityThreadClass = mActivityThread.getClass();
            Field mActivitiesField = mActivityThreadClass.getDeclaredField("mActivities");
            mActivitiesField.setAccessible(true);
            Object mActivities = mActivitiesField.get(mActivityThread);
            // 注意这里一定写成Map，低版本这里用的是HashMap，高版本用的是ArrayMap
            if (mActivities instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> arrayMap = (Map<Object, Object>) mActivities;
                for (Map.Entry<Object, Object> entry : arrayMap.entrySet()) {
                    Object value = entry.getValue();
                    Class<?> activityClientRecordClass = value.getClass();
                    Field activityField = activityClientRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Object o = activityField.get(value);
                    list.add((Activity) o);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            list = null;
        }
        return list;
    }


    /**
     * 获得Fresco相关信息
     *
     * @param context 上下文对象
     * @return Fresco相关信息
     */
    private static String getFrescoInfo(Context context) {
        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject.put("fileCache", BytesUtil.formatFileSizeByBytes(context, Fresco.getImagePipelineFactory().getMainFileCache().getSize()));//获取磁盘上的文件大小，需要添加DiskCacheConfig.setIndexPopulateAtStartupEnabled(true)这行代码，否则结果为-1
//            jsonObject.put("bitmapCache", BytesUtil.formatFileSizeByBytes(context, Fresco.getImagePipelineFactory().getBitmapCountingMemoryCache().getSizeInBytes()));//获取内存中所有当前缓存项的总大小（以字节为单位）。
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return jsonObject.toString();
    }

    /**
     * 获取线程相关信息
     *
     * @return 线程相关信息
     */
    private static String getThreadInfo() {
        JSONObject threadInfo = new JSONObject();
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();

        Iterator<Thread> keyIterator = traces.keySet().iterator();
        List<String> threadNameList = new ArrayList<>(traces.size());
        while (keyIterator.hasNext()) {
            threadNameList.add(keyIterator.next().getName());
        }
        try {
            threadInfo.put("threadCount", traces.size());
            threadInfo.put("threadNameList", threadNameList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return threadInfo.toString();
    }

    /**
     * 获取fd信息
     *
     * @return fd信息
     */
    private static String getFdInfo() {
        JSONObject fdInfo = new JSONObject();
        File fdFile = new File("/proc/" + Process.myPid() + "/fd");
        int fdCount = 0;
        List<String> fdNameList = new ArrayList<>();
        if (fdFile.exists()) {
            File[] fileList = fdFile.listFiles();
            fdCount = fileList.length;
            for (File file : fileList) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        fdNameList.add(Os.readlink(file.getAbsolutePath()));
                    } else {
                        fdNameList.add(file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fdNameList.add(file.getAbsolutePath());
                }
            }
        }
        try {
            fdInfo.put("fdCount", fdCount);
            fdInfo.put("fdNameList", fdNameList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fdInfo.toString();
    }


    /**
     * 获取gc相关信息
     */
    private static String getGCInfo() {
        JSONObject json = new JSONObject();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 运行的GC次数
                String gcCount = Debug.getRuntimeStat("art.gc.gc-count");
                // GC使用的总耗时，单位是毫秒
                String gcTime = Debug.getRuntimeStat("art.gc.gc-time");
                // 阻塞式GC的次数
                String gcBlockingCount = Debug.getRuntimeStat("art.gc.blocking-gc-count");
                // 阻塞式GC的总耗时
                String gcBlockingTime = Debug.getRuntimeStat("art.gc.blocking-gc-time");
                json.put("gcCount", gcCount);
                json.put("gcTime", gcTime);
                json.put("gcBlockingCount", gcBlockingCount);
                json.put("gcBlockingTime", gcBlockingTime);
            } else {
                int allocCount = Debug.getGlobalAllocCount();
                int allocSize = Debug.getGlobalAllocSize();
                int gcCount = Debug.getGlobalGcInvocationCount();
                json.put("allocCount", allocCount);
                json.put("allocSize", allocSize);
                json.put("gcCount", gcCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json.toString();

    }
}
