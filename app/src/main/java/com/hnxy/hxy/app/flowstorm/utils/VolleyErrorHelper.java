package com.hnxy.hxy.app.flowstorm.utils;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.hnxy.hxy.app.flowstorm.R;

/**
 * Created by Administrator on 2015/6/19.
 */
public class VolleyErrorHelper {
    //用于返回具体错误信息，分辨错误类别
    public static String getMessage(Object error, Context context) {
        if (error == null) {
            return context.getResources().getString(R.string.error_server_stop);
        }
       System.out.println(error.toString());
        if (error instanceof TimeoutError) {
            return context.getResources().getString(R.string.error_timeout);
        } else if (isServerProblem(error)) {
            return handleServerError(error, context);
        } else if (isNetworkProblem(error)) {
            return context.getResources().getString(R.string.error_no_internet);
        }
        return context.getResources().getString(R.string.error_server_stop);
    }

    //判断是否是网络错误
    private static boolean isNetworkProblem(Object error) {
        return (error instanceof NetworkError) ||
                (error instanceof NoConnectionError);
    }

    //判断是否是服务端错误
    private static boolean isServerProblem(Object error) {
        return (error instanceof ServerError) ||
                (error instanceof AuthFailureError);
    }

    //处理服务端错误
    private static String handleServerError(Object err, Context context) {
        VolleyError error = (VolleyError) err;

        NetworkResponse response = error.networkResponse;

        if (response != null) {
            switch (response.statusCode) {
                case 404:
                case 422:
                case 401:
                    try {
                        // server might return error like this { "error": "Some error occured" }
                        // Use "Gson" to parse the result
                        //                        HashMap<String, String> result = new Gson().fromJson(new String(response.data),
                        //                                new TypeToken<Map<String, String>>() {}.getType());
                        //                        JSONObject  object = new JSONObject(new String(response.data));
                        //                        if (result != null && result.containsKey("error")) {
                        //                            return result.get("error");
                        //                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return error.getMessage();

                default:
                    return context.getResources().getString(R.string.error_timeout);
            }
        }
        return context.getResources().getString(R.string.error_server_stop);
    }

}
