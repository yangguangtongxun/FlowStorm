package com.hnxy.hxy.app.flowstorm.ui;

import android.os.Environment;
import android.test.InstrumentationTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Administrator on 2015/9/20.
 */
public class TestClass extends InstrumentationTestCase {
    /**
     * 事先预执行代码
     *
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testDat() {

        JSONObject root = new JSONObject();
        try {
            root.put("version", 1);
            root.put("count", 1);
            JSONArray profiles = new JSONArray();
            JSONObject profile = new JSONObject();
            profile.put("name", "client.ovpn");
            profile.put("content", "client\n" +
                    "dev tun\n" +
                    "proto tcp\n" +
                    "remote 125.46.58.29 1194\n" +
                    "resolv-retry infinite\n" +
                    "nobind\n" +
                    "persist-key\n" +
                    "persist-tun\n" +
                    "ca ca.crt\n" +
                    "auth-user-pass\n" +
                    "comp-lzo\n" +
                    "verb 3");
            profiles.put(profile);
            root.put("profiles", profiles);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
            gzip.write(root.toString().getBytes());
            gzip.close();
            long date = System.currentTimeMillis();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(longToByteArray(date), 0, longToByteArray(date).length);
            FileOutputStream fileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/profile.dat");
            fileOutputStream.write(longToByteArray(date), 0, longToByteArray(date).length);
            fileOutputStream.write(outputStream.toByteArray(), 0, outputStream.toByteArray().length);
            fileOutputStream.flush();
            fileOutputStream.close();
            System.out.println("date: " + out.toString() + "data: " + outputStream.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将64位的long值放到8字节的byte数组
     *
     * @param num
     * @return 返回转换后的byte数组
     */
    public static byte[] longToByteArray(long num) {
        byte[] result = new byte[8];
        result[0] = (byte) (num >>> 56);// 取最高8位放到0下标
        result[1] = (byte) (num >>> 48);// 取最高8位放到0下标
        result[2] = (byte) (num >>> 40);// 取最高8位放到0下标
        result[3] = (byte) (num >>> 32);// 取最高8位放到0下标
        result[4] = (byte) (num >>> 24);// 取最高8位放到0下标
        result[5] = (byte) (num >>> 16);// 取次高8为放到1下标
        result[6] = (byte) (num >>> 8); // 取次低8位放到2下标
        result[7] = (byte) (num); // 取最低8位放到3下标
        return result;
    }
}
