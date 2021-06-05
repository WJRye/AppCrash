@[toc]

---
## App Crash

在应用程序Crash的时候， 我们通常想收集一些信息来帮助分析本次Crash。下面列举了一些在应用程序Crash的时候，应该收集的一些基本信息。

注意：此次在应用程序Crash时收集的信息，只用于帮助分析**OutOfMemoryError**异常，对其它异常只做参考。

### 发生OOM的Java虚拟机内存区域

Java虚拟机内存区域划分：

 - 程序计数器：当前线程所执行的字节码的型号指示器。
 - Java虚拟机栈：每个方法在执行的同时都会创建一个栈帧用于存储局部变量表，操作数栈，动态链接，方法出口等信息。
 - 本地方法栈：于Java虚拟机一样，本地方法栈区域只是为Native方法服务。
 -  Java堆：对象实例和数组。
 - 方法区：用于存储已被虚拟机家在的类信息、常量、静态变量、即时编译器后的代码等数据。
 - 运行常量池：用于存放编译器生成的各种字面量和符号引用。

Java虚拟机会发生的OutOfMemoryError的内存区域：

 - Java虚拟机栈：如果虚拟机可以动态扩展（当前大部分的Java虚拟机都可动态扩展，只不过Java虚拟机规范中也允许固定长度的虚拟机栈），如果扩展时无法申请到足够的内存，就会抛出OutOfMemoryError。
 - 本地方法栈：与Java虚拟机栈一样。
 -  Java堆：如果在堆中没有内存完成实例分配，并且也无法再扩展时，就会抛出OutOfMemoryError异常。
 - 方法区：当方法区无法满足内存分配需求时，将抛出OutOfMemoryError异常。
 - 运行常量池：当常量池无法再申请到内存会抛出OutOfMemoryError异常。

---

### 如何收集

通过接口`Thread.UncaughtExceptionHandler`可以知道应用程序Crash时机，并在它的接口回调方法`uncaughtException`中获取异常信息。Thread.UncaughtExceptionHandler 的作用如下：

 - 当线程因未捕获的异常而突然终止时调用的处理程序接口；
 - 当线程由于未捕获的异常而即将终止时，Java虚拟机将使用{@link #getUncaughtExceptionHandler}向线程查询其UncaughtExceptionHandler，并将调用处理程序的 uncaughtException方法，将线程和异常作为参数传递；
 - 如果某个线程没有显式设置其 UncaughtExceptionHandler，则其 ThreadGroup 对象将充当其 UncaughtExceptionHandler。 如果ThreadGroup对象没有处理异常的特殊要求，它可以将调用转发到{@linkplain #getDefaultUncaughtExceptionHandler默认的未捕获异常处理程序}。

```
    @FunctionalInterface
    public interface UncaughtExceptionHandler {
         /**
         * 当给定线程由于给定的未捕获异常而终止时调用的方法。
         * <p>
         * Java虚拟机将忽略此方法抛出的任何异常。
         * @param t the thread
         * @param e the exception
         */
        void uncaughtException(Thread t, Throwable e);
    }
```

在应用程序中实现Thread.UncaughtExceptionHandler接口：

```
object CrashHandler : Thread.UncaughtExceptionHandler {


    private var mApplication: Application? = null

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        CrashInfo.getAll(mApplication) //收集信息
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(t, e)
    }

    fun init(application: Application) {
        mApplication = application
        Thread.setDefaultUncaughtExceptionHandler(this)//当线程由于未捕获的异常而即将终止时，由当前类来处理
        CrashInfo.track(application) //收集信息
    }
}
```
在Application的onCreate方法中添加Crash处理：

```
public class YourApplication extents Application
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.INSTANCE.init(this);
    }
}
```
### 收集哪些信息

在应用程序crash时，除了基本的异常栈信息外，可能还需要一些更加详细的设备基本信息，应用程序内存信息，系统内存信息，应用程序Activity栈信息等来帮助分析Crash。

#### 设备基本信息

设备基本信息包括：手机品牌，手机品牌类型，手机制造商，手机系统版本，手机SDK版本，手机屏幕分辨率，手机屏幕密度。

```
    /**
     * 设备基本信息
     *
     * @param context 上下文对象
     * @return JSON字符串
     */
    public static String getDeviceInfo(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        JSONObject dmJSON = new JSONObject();
        try {
            dmJSON.put("brand", Build.BRAND); //手机品牌
            dmJSON.put("release", Build.VERSION.RELEASE);//手机系统版本
            dmJSON.put("model", Build.MODEL); //手机品牌类型
            dmJSON.put("manufacturer", Build.MANUFACTURER); //手机制造商
            dmJSON.put("sdk", Build.VERSION.SDK); //手机SDK版本
            dmJSON.put("width*height", new StringBuilder().append(dm.widthPixels).append('*').append(dm.heightPixels).toString()); //手机屏幕分辨率
            dmJSON.put("densityDpi", String.valueOf(dm.densityDpi)); //手机屏幕密度
            WindowManager windowManager =
                    (WindowManager) context.getSystemService(Context.
                            WINDOW_SERVICE);
            if (windowManager != null) {//手机是否含有虚拟键盘
                final Display display = windowManager.getDefaultDisplay();
                Point outPoint = new Point();
                if (Build.VERSION.SDK_INT >= 19) {
                    // 可能有虚拟按键的情况
                    display.getRealSize(outPoint);
                } else {
                    // 没有虚拟按键
                    display.getSize(outPoint);
                }
                dmJSON.put("realWidth*realHeight", new StringBuilder().append(outPoint.x).append('*').append(outPoint.y).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dmJSON.toString();
    }
```

#### 应用程序内存信息

应用程序内存信息包括：系统给应用程序分配的最大内存，Java虚拟机将尝试使用的最大内存量，当前可用于当前和未来对象的内存总量等。

 - `ActivityManager#getMemoryClass`：**系统能给应用程序分配的最大内存**。
 - `ActivityManager#getLargeMemoryClass`：**在LargeHeap下，系统能给应用程序分配的最大内存**。
 - `Runtime#maxMemory`：返回Java虚拟机将尝试使用的最大内存量。 如果没有固有限制，则返回值{@link java.lang.Long #MAX_VALUE}。
 - `Runtime#totalMemory`：返回Java虚拟机中的内存总量。 此方法返回的值可能会随时间而变化，具体取决于主机环境。请注意，保存任何给定类型的对象所需的内存量可能与实现有关。当前可用于当前和未来对象的内存总量，以字节为单位。
 - `Runtime#freeMemory`：返回Java虚拟机中的可用内存量。 调用gc方法可能会导致freeMemory返回的值增加。当前可用于未来分配对象的内存总量的近似值，以字节为单位。
 - `Runtime#totalMemory - Runtime#freeMemory`：当前已使用内存。

```
    /**
     * 程序运行时内存信息
     *
     * @param context 上下文对象
     * @return
     */
    public static String getAppMemory(Context context) {
        JSONObject appMemoryJSON = new JSONObject();
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                appMemoryJSON.put("memoryClass", BytesUtil.formatFileSizeByMB(context, am.getMemoryClass()));//系统能给应用程序分配的最大内存
                appMemoryJSON.put("largeMemoryClass", BytesUtil.formatFileSizeByMB(context, am.getLargeMemoryClass()));//在LargeHeap下，系统能给应用程序分配的最大内存
                if (DEBUG) {
                    Log.d(TAG, "getAppMemory-->getMemoryClass=" + am.getMemoryClass());
                    Log.d(TAG, "getAppMemory-->getLargeMemoryClass=" + am.getLargeMemoryClass());
                }
            }
            Runtime r = Runtime.getRuntime();
            appMemoryJSON.put("maxMemory", BytesUtil.formatFileSizeByBytes(context, r.maxMemory()));//返回Java虚拟机将尝试使用的最大内存量。 如果没有固有限制，则返回值{@link java.lang.Long #MAX_VALUE}。
            appMemoryJSON.put("totalMemory", BytesUtil.formatFileSizeByBytes(context, r.totalMemory()));//当前可用内存
            appMemoryJSON.put("freeMemory", BytesUtil.formatFileSizeByBytes(context, r.freeMemory()));//当前空闲内存
            appMemoryJSON.put("usedMemory", BytesUtil.formatFileSizeByBytes(context, r.totalMemory() - r.freeMemory()));//当前已使用内存
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appMemoryJSON.toString();
    }
```
#### 系统内存信息

系统内存信息包括：系统总内存，系统剩余内存，系统是否处于低内存等。

 - `ActivityManager.MemoryInfo.totalMem`：内核可访问的总内存。 这基本上是设备的RAM大小，不包括DMA缓冲区之类的内核固定分配，基带CPU的RAM等。
 - `ActivityManager.MemoryInfo.availMem`：系统上的可用内存。 这个数字不应该被认为是绝对的：由于内核的性质，这个内存的很大一部分实际上在使用，并且需要整个系统运行良好。
 - `ActivityManager.MemoryInfo.threshold`：**表示低内存阈值，低于这个阈值，就表示内存很低并开始查杀后台服务和其他非外部进程**。
 - **ActivityManager.MemoryInfo.lowMemory**：**如果系统认为自己当前处于低内存状态，则设置为true**。

```
    /**
     * 系统内存信息
     *
     * @param context 上下文对象
     * @return
     */
    public static String getDeviceSystemMemory(Context context) {
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
```
#### 应用程序运行时的虚拟机信息

应用程序运行时的虚拟机信息包括：java-heap，native-heap，code，stack，graphics，private-other，system，total-pss，total-swap。

关于PSS定义：按比例分配占用内存 (PSS)

> 这表示您的应用的 RAM 使用情况，考虑了在各进程之间共享 RAM 页的情况。您的进程独有的任何 RAM 页会直接影响其 PSS 值，而与其他进程共享的 RAM 页仅影响与共享量成比例的 PSS 值。例如，两个进程之间共享的 RAM 页会将其一半的大小贡献给每个进程的 PSS。

PSS 结果一个比较好的特性是，您可以将所有进程的 PSS 相加来确定所有进程正在使用的实际内存。这意味着 PSS 适合测定进程的实际 RAM 比重和比较其他进程的 RAM 使用情况与可用总 RAM。

 - `java-heap`：从 Java 或 Kotlin 代码分配的对象内存。
 - `native-heap`：从 C 或 C++ 代码分配的对象内存。（即使您的应用中不使用 C++，您也可能会看到此处使用的一些原生内存，因为 Android 框架使用原生内存代表您处理各种任务，如处理图像资源和其他图形时，即使您编写的代码采用 Java 或 Kotlin 语言。）
 -  `code`：您的应用用于处理代码和资源（如 dex 字节码、已优化或已编译的 dex 码、.so 库和字体）的内存。
 -   `stack`： 您的应用中的原生堆栈和 Java 堆栈使用的内存。 这通常与您的应用运行多少线程有关。
 - `graphics`：**图形缓冲区队列向屏幕显示像素（包括 GL 表面、GL 纹理等等）所使用的内存**。 （请注意，这是与 CPU 共享的内存，不是 GPU 专用内存。）
 - `total-pss`： 包括所有 Zygote 分配（如上述 PSS 定义所述，通过进程之间的共享内存量来衡量）


利用 Debug.MemoryInfo#getMemoryStats方法获取：

```
   /**
     * 调试下程序运行时内存信息
     *
     * @param context 上下文对象
     * @return
     */
    @TargetApi(value = Build.VERSION_CODES.M)
    public static String getDebugAppMemory(Context context) {
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
                if (DEBUG)
                    Log.d(TAG, "getDebugAppMemory-->key=" + entry.getKey() + ";getValue=" + entry.getValue() + ";value=" + value);
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
```
#### 设备SD卡内存信息

设备SD卡内存信息包括：SDCard总内存，SDCard剩余内存。

 - Environment.getExternalStorageDirectory().getTotalSpace()：SDCard总内存。
 - Environment.getExternalStorageDirectory().getUsableSpace()：SDCard剩余内存。

```
    /**
     * 设备SD卡内存
     *
     * @param context 上下文对象
     * @return
     */
    public static String getDeviceSDCardMemory(Context context) {
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
```
#### 获取内存快照

应用程序运行时，将内存快照dump到.hprof文件中。

 - Debug.dumpHprofData：将“hprof”数据转储到指定的文件。 这可能会导致GC。

```
    /**
     * 获取内存快照
     *
     * @param pathname 存储文件路径
     */
    public static void dumpToFile(String pathname) {

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
            if (DEBUG)
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
        }

    }
```
文件以YYYY-MM-DD-HH-MM-SS.hprof形式存储，如：2018-12-10-10-23-12.hprof。

#### 获取应用程序Activity信息

[通过反射获取应用程序的Activity栈信息](https://www.jianshu.com/p/ac0b237bac03)（低版本得到的顺序可能不是Acitivity栈顺序，高版本得到的是Acitivity栈顺序，具体情况根据系统版本而定）：
```
    /**
     * 通过反射获取应用程序的Activity栈信息
     *
     * @param application 应用程序对象
     * @return Activity栈列表
     */
    public static List<Activity> getActivitiesByApplication(Application application) {
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
            if (DEBUG)
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
        }
        if (DEBUG && list != null) {
            Iterator<Activity> iterator = list.iterator();
            while (iterator.hasNext()) {
                Log.d(TAG, "getActivitiesByApplication-->value=" + iterator.next());
            }
        }
        return list;
    }
```
除了获取到Activity栈信息，还可以获取Activity中的Fragment信息：

```
    /**
     * Activity栈信息
     *
     * @param application     应用程序对象
     * @param includeFragment true包括Fragment，否则不包括
     * @return
     */
    public static String getActivities(Application application, boolean includeFragment) {
        List<Activity> list = getActivitiesByApplication(application);
        Log.d(TAG, "getActivities-->list=" + list + ";includeFragment=" + includeFragment);
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
            if (DEBUG) {
                Log.d(TAG, "getActivities-->activity-->name=" + activity.getClass().getName() + ";includeFragment=" + includeFragment);
            }
            if (includeFragment) {
                Map<String, List<String>> temp = null;
                if (activity instanceof FragmentActivity) {
                    temp = getSupportFragmentOfActivity(activity.getClass().getName(), ((FragmentActivity) activity).getSupportFragmentManager());//是support包中的Fragment
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    temp = getFragmentOfActivity(activity.getClass().getName(), activity.getFragmentManager());//是app包中的Fragment
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

   //Fragment嵌套是一个树形结构，所以相当于按顺序层级遍历树
    @TargetApi(Build.VERSION_CODES.O)
    private static Map<String, List<String>> getFragmentOfActivity(String
                                                                           name, android.app.FragmentManager fragmentManager) {
        Map<String, android.app.FragmentManager> fragmentManagers = new LinkedHashMap<>();
        fragmentManagers.put(name, fragmentManager);//存储根节点
        Map<String, android.app.FragmentManager> childFragmentManagers = null;
        Map<String, List<String>> result = new LinkedHashMap<>();
        int size = 4;
        while (!fragmentManagers.isEmpty()) { //开始遍历每一层的节点
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
                    if (DEBUG) {
                        Log.d(TAG, "getFragmentOfActivity-->key=" + entry.getKey() + ";value[" + index + "]=" + fragmentName);
                    }
                    if (!fragment.getChildFragmentManager().getFragments().isEmpty()) {
                        childFragmentManagers.put(values.contains(fragmentName) ? fragmentName + index : fragmentName, fragment.getChildFragmentManager()); //存储下一层的节点
                    }
                    values.add(fragmentName);
                    index++;
                }
                result.put(entry.getKey(), values);
            }

            fragmentManagers.clear();
            if (!childFragmentManagers.isEmpty())
                fragmentManagers.putAll(childFragmentManagers);//下一层节点
        }

        return result;

    }

    //和上面方法一样，只不过是support包中的Fragment
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
                    if (DEBUG) {
                        Log.d(TAG, "getSupportFragmentOfActivity-->key=" + entry.getKey() + ";value[" + index + "]=" + fragmentName);
                    }
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

        if (DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.d(TAG, "getSupportFragmentOfActivity-->json=" + JSONObject.wrap(result));
        }

        return result;

    }
```
#### 应用程序是冷启动还是热启动

在Application中可以注册Activity生命周期的监听器。应用程序初始化时，ActivityCount（Activity的个数）为-1，当有Activity调用onStart时，如果ActivityCount为-1，则将ActivityCount置为0，并马上开始记录启动Activity的个数（ActivityCount++）；在Activity调用onStop时，ActivityCount减一（ActivityCount--）；因此当应用程序退至后台时，ActivityCount为0（所有的Activity都会至少处于onStop状态），当再次启动应用程序时，如果ActivityCount为0，**则说明此次就是热启动了。**

```
/**
 * 检测应用程序是冷启动还是热启动
 */
class AppColdStart {

    private constructor()

    companion object {
        private var mActivityCount: Int = -1//记录启动的Activity

        private var mIsColdStart: Boolean = true //是否是冷启动
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
                mIsColdStart = false //表示已经启动过
            } else if (mActivityCount == -1) {
                mActivityCount = 0 //表示初次启动
            }
            mActivityCount++
        }

        override fun onActivityStopped(activity: Activity?) {
            super.onActivityStopped(activity)
            mActivityCount--
        }

    }
}
```

#### 获取线程信息
通过`Thread.getAllStackTraces`获取相关线程信息（线程数目，线程名称列表）：
```
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
```

#### 获取fd信息
通过读取文件`/proc/" + Process.myPid() + "/fd`，获取当前应用程序相关的fd信息（fd数目，fd名称列表）：
```
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
```
#### 获取GC信息
主要是关注的信息是：运行的GC次数，GC使用的总耗时，阻塞式GC的次数，阻塞式GC的总耗时等
```
    /**
     * 获取gc相关信息
     */
    private fun getGCInfo(): String {
        val json = JSONObject()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 运行的GC次数
                val gcCount: String? = Debug.getRuntimeStat("art.gc.gc-count")
                // GC使用的总耗时，单位是毫秒
                val gcTime: String? = Debug.getRuntimeStat("art.gc.gc-time")
                // 阻塞式GC的次数
                val gcBlockingCount: String? = Debug.getRuntimeStat("art.gc.blocking-gc-count")
                // 阻塞式GC的总耗时
                val gcBlockingTime: String? = Debug.getRuntimeStat("art.gc.blocking-gc-time")
                json.put("gcCount", gcCount)
                json.put("gcTime", gcTime)
                json.put("gcBlockingCount", gcBlockingCount)
                json.put("gcBlockingTime", gcBlockingTime)
            } else {
                val allocCount = Debug.getGlobalAllocCount().toLong()
                val allocSize = Debug.getGlobalAllocSize().toLong()
                val gcCount = Debug.getGlobalGcInvocationCount().toLong()
                json.put("allocCount", allocCount)
                json.put("allocSize", allocSize)
                json.put("gcCount", gcCount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return json.toString()
    }
```

#### 其他信息

除了上面的信息外，还可以统计用户使用应用程序的时间，在各个Activity停留的时间信息等，具体可查看Github地址[AppCrash](https://github.com/WJRye/AppCrash)中的[TrackActivity](https://github.com/WJRye/AppCrash/blob/master/crash/TrackActivity.java)类。


