package com.hnxy.hxy.app.flowstorm.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.JsonReader;

import com.hnxy.hxy.app.flowstorm.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import de.blinkt.openvpn.core.OpenVPNService;

/**
 * API客户端类
 *
 * Created by hu60 on 2015-08-30.
 */
public class ApiClient {
    private final String apiBaseUrl = "http://user.ip2o.com:8080/user/q.php/";
    private final String accountCountUrl = apiBaseUrl + "api.count.json";
    private final String newVersionUrl = apiBaseUrl + "api.update.json";//版本更新接口
    private Context context;

    public ApiClient(Context context) {
        this.context = context;
    }

    private String urlEncode(String baseUrl, HashMap<String, String>args) throws ApiException {
        StringBuffer url = new StringBuffer(baseUrl);
        int i = 0;

        for (Map.Entry<String, String> arg : args.entrySet()) {
            if (i == 0) {
                url.append('?');
            } else {
                url.append('&');
            }

            try {
                url.append(URLEncoder.encode(arg.getKey(), "UTF-8"));
                url.append('=');
                url.append(URLEncoder.encode(arg.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new ApiException("设备不支持UTF-8编码");
            }

            i ++;
        }

        return url.toString();
    }

    private byte[] httpGet(String baseUrl, HashMap<String, String>args) throws ApiException {
        URL url;
        HttpURLConnection urlConnection;
        InputStream stream;
        ByteArrayOutputStream response;
        byte[] buffer;
        int len;


        try {
            url = new URL(urlEncode(baseUrl, args));
        } catch (MalformedURLException e) {
            throw new ApiException("URL格式错误");
        }

        try {
            urlConnection = (HttpURLConnection)url.openConnection();
        } catch (IOException e) {
            throw new ApiException("连接失败");
        }

        try {
            stream = urlConnection.getInputStream();
            response = new ByteArrayOutputStream();
            buffer = new byte[1024];
            len = 0;

            while (-1 != (len = stream.read(buffer))) {
                response.write(buffer, 0, len);
            }

        } catch (IOException e) {
            throw new ApiException("读取响应失败");
        }

        return response.toByteArray();
    }

    public AccountCounter accountCount(Account account) throws ApiException {
        HashMap<String, String>args = new HashMap<>();

        if (account.mUsername != null && !"".equals(account.mUsername)) {
            args.put("username", account.mUsername);
        } else {
            throw new ApiException("用户名未设置");
        }

        if (account.mTransientPW != null) {
            args.put("password", account.mTransientPW);
        } else if (account.mPassword != null) {
            args.put("password", account.mPassword);
        } else {
            throw new ApiException("密码未设置");
        }

        byte[] response = httpGet(accountCountUrl, args);
        JsonReader reader;
        String key;
        AccountCounter accountCounter = new AccountCounter();

        try {
            reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ApiException("设备不支持UTF-8编码");
        }

        try {
            reader.beginObject();

            while (reader.hasNext()) {
                key = reader.nextName();

                switch (key) {
                    case "maxGlobalTraffic":
                        accountCounter.maxGlobalTraffic = reader.nextLong();
                        break;

                    case "globalTrafficCount":
                        accountCounter.globalTrafficCount = reader.nextLong();
                        break;

                    case "maxActiveDays":
                        accountCounter.maxActiveDays = reader.nextLong();
                        break;

                    case "activeDaysCount":
                        accountCounter.activeDaysCount = reader.nextLong();
                        break;

                    case "error":
                        accountCounter.error = reader.nextBoolean();
                        break;

                    case "message":
                        accountCounter.errMsg = reader.nextString();
                        break;

                    case "code":
                        accountCounter.errCode = reader.nextLong();
                        break;
                }
            }

            reader.endObject();
        } catch (IOException e) {
            throw new ApiException("处理结果时错误");
        }

        return accountCounter;
    }


    public NewVersion checkNewVersion() throws ApiException {
        HashMap<String, String>args = new HashMap<>();
        String version = "";

        try {
            PackageInfo packageinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageinfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            //忽略异常
        }

        args.put("name", context.getString(R.string.version_name));
        args.put("version", version);

        byte[] response = httpGet(newVersionUrl, args);
        JsonReader reader;
        String key;
        NewVersion newVersion = new NewVersion();

        try {
            reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ApiException("设备不支持UTF-8编码");
        }

        try {
            reader.beginObject();

            while (reader.hasNext()) {
                key = reader.nextName();

                switch (key) {
                    case "newVersion":
                        newVersion.newVersion = reader.nextBoolean();
                        break;

                    case "name":
                        newVersion.name = reader.nextString();
                        break;

                    case "version":
                        newVersion.version = reader.nextString();
                        break;

                    case "introd":
                        newVersion.introd = reader.nextString();
                        break;

                    case "url":
                        newVersion.url = reader.nextString();
                        break;

                    case "size":
                        newVersion.size = reader.nextLong();
                        break;

                    case "error":
                        newVersion.error = reader.nextBoolean();
                        break;

                    case "message":
                        newVersion.errMsg = reader.nextString();
                        break;

                    case "code":
                        newVersion.errCode = reader.nextLong();
                        break;
                }
            }

            reader.endObject();
        } catch (IOException e) {
            throw new ApiException("处理结果时错误");
        }

        return newVersion;
    }

    /**
     * 帐号统计信息
     */
    public static class AccountCounter {
        public long maxGlobalTraffic = 0;
        public long globalTrafficCount = 0;
        public long maxActiveDays = 0;
        public long activeDaysCount = 0;
        public boolean error = false;
        public String errMsg = null;
        public long errCode = 0;
    }

    /**
     * 新版本信息
     */
    public static class NewVersion {
        public boolean newVersion = false;
        public String name = null;
        public String version = null;
        public String introd = null;
        public String url = null;
        public long size = 0;
        public boolean error = false;
        public String errMsg = null;
        public long errCode = 0;

        public String getSize() {
            return OpenVPNService.humanReadableByteCount(size, false);
        }
    }

    /**
     * API异常类
     */
    public static class ApiException extends Exception {
        public ApiException(String msg) {
            super(msg);
        }
    }
}
