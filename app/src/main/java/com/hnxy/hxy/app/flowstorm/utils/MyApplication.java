package com.hnxy.hxy.app.flowstorm.utils;

import android.app.Activity;
import android.app.Application;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/8/4.
 */
public class MyApplication extends Application {



    public static List<Activity> activities = new ArrayList<>();
    // 创建一个TAG，方便调试或Log
    private static final String TAG = MyApplication.class.getSimpleName();
    // 创建一个全局的请求队列
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    // 创建一个static ApplicationController对象，便于全局访问
    private static MyApplication mInstance;

    // NetErrorReceiver netErrorReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
//        mMyLocationListener = new MyLocationListener();
//        initLocation();
//        mLocationClient.registerLocationListener(mMyLocationListener);
    }

    /**
     * 以下为需要我们自己封装的添加请求取消请求等方法
     */

    /**
     * 用于返回一个ApplicationController单例
     *
     * @return
     */
    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    /**
     * 用于返回全局RequestQueue对象，如果为空则创建它
     *
     * @return
     */
    public RequestQueue getRequestQueue() {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        return requestQueue;
    }

    /**
     * 将Request对象添加进RequestQueue，由于Request有*StringRequest,JsonObjectResquest...
     * 等多种类型，所以需要用到*泛型。同时可将*tag作为可选参数以便标示出每一个不同请求
     */

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // 如果tag为空的话，就是用默认TAG
        req.setRetryPolicy(new DefaultRetryPolicy(6 * 1000, 1, 1.0f));
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        System.out.println("path: " + req.getUrl());
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setRetryPolicy(new DefaultRetryPolicy(6 * 1000, 1, 1.0f));
        req.setTag(TAG);
        System.out.println("path: " + req.getUrl());
        getRequestQueue().add(req);
    }

    // 通过各Request对象的Tag属性取消请求
    public void cancelPendingRequests(Object tag) {
        if (requestQueue != null) {
            requestQueue.cancelAll(tag);
        }
    }

    public ImageLoader getImageLoader() {
        getRequestQueue();
        if (imageLoader == null) {
            imageLoader = new ImageLoader(requestQueue, new LruBitmapCache());
        }
        return imageLoader;
    }

    public void cancelPendingRequest(Object tag) {
        if (requestQueue != null) {
            requestQueue.cancelAll(tag);
        }
    }

    public static void exitApp() {
        for (Activity activity : activities) {
            activity.finish();
        }
    }




}
