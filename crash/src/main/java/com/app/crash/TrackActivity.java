package com.app.crash;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


final class TrackActivity {
    private static List<TrackActivity> sTrack = new ArrayList<>();
    private String name;
    private long startTime;
    private long endTime;
    private static final char SPLIT_CHAR = ':';

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private static final String TAG = "TrackActivity";

    private static boolean DEBUG = false;


    private String getActivityTime() {
        return getTime(startTime, endTime);
    }

    private static String getTime(long startTime, long endTime) {
        long duration = endTime - startTime;
        int seconds = (int) Math.floor(duration / 1000);
        int days = 0, hours = 0, minutes = 0;

        if (seconds > SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds > SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds > SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days);
            sb.append(SPLIT_CHAR);
        }
        sb.append(hours);
        sb.append(SPLIT_CHAR);
        sb.append(minutes);
        sb.append(SPLIT_CHAR);
        sb.append(seconds);
        return sb.toString();
    }


    @Override
    public String toString() {
        return new StringBuilder().append(name).append(" ").append(getActivityTime()).toString();
    }


    static void track(Application application, boolean debug) {
        DEBUG = debug;
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksAdapter() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                TrackActivity trackActivity = new TrackActivity();
                trackActivity.startTime = System.currentTimeMillis();
                trackActivity.name = activity.getClass().getName();
                sTrack.add(trackActivity);

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                for (int i = sTrack.size() - 1; i >= 0; i--) {
                    TrackActivity trackActivity = sTrack.get(i);
                    if (activity.getClass().getName().equals(trackActivity.name)) {
                        trackActivity.endTime = System.currentTimeMillis();
                        break;
                    }
                }
            }
        });
    }

    /**
     * 跟踪用户
     *
     * @return
     */
    static String getTrackActivityPath() {
        if (sTrack == null || sTrack.isEmpty())
            return null;
        JSONObject jsonObject = new JSONObject();
        Map<String, Pair<Long, Long>> map = new HashMap<>();
        long startTime = 0L;
        long endTime = 0L;
        for (TrackActivity info : TrackActivity.sTrack) {
            if (info.endTime == 0) info.endTime = System.currentTimeMillis();
            Pair<Long, Long> pair = map.get(info.name);
            if (pair != null) {
                startTime = pair.first + info.startTime;
                endTime = pair.second + info.endTime;
                map.put(info.name, Pair.create(startTime, endTime));
                try {
                    jsonObject.put(info.name, TrackActivity.getTime(startTime, endTime));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                map.put(info.name, Pair.create(info.startTime, info.endTime));
                try {
                    jsonObject.put(info.name, info.getActivityTime());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        map.clear();
        return jsonObject.toString();
    }


    /**
     * 跟踪用户
     *
     * @return
     */
    static String getTrackActivityPathDetail() {
        if (sTrack == null || sTrack.isEmpty())
            return null;
        JSONArray jsonArray = new JSONArray();
        Iterator<TrackActivity> iterator = sTrack.iterator();
        TrackActivity trackActivity = null;
        while (iterator.hasNext()) {
            trackActivity = iterator.next();
            if (trackActivity.endTime == 0) {
                trackActivity.endTime = System.currentTimeMillis();
            }
            jsonArray.put(trackActivity.toString());
        }
        return jsonArray.toString();
    }
}
