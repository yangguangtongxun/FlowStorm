package com.hnxy.hxy.app.flowstorm.ui;

import android.app.Activity;
import android.content.Context;

import com.hnxy.hxy.app.flowstorm.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;

/**
 * Created by hu60 on 15-8-18.
 */
public class ProfileLoader {
    private Context context;
    private final static String profileDataName = "profile.dat";
    private final static String profileOutputName = "profile";


    public ProfileLoader(Context context) {
        this.context = context;
    }

    /**
     * 从以下格式的JSON数据中读取Vpn配置文件并加入配置管理器
     * <p/>
     * {
     * "version": 1
     * "count": 数量,
     * "profiles": [
     * {
     * "name": 配置文件名,
     * "content": 配置文件内容
     * },
     * ...
     * ]
     * }
     * <p/>
     * 配置文件存储格式：
     * 前8字节：long型，配置文件生成时的系统时间毫秒数（System.currentTimeMillis()的返回值）
     * 后续字节：gzip压缩的JSON字符串数据
     *
     * @param reader 数据文件
     * @return 失败个数
     */
    public Status load(Reader reader) throws ProfileException {
        try {
            BufferedReader br = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            JSONObject data = new JSONObject(new JSONTokener(sb.toString()));
            JSONArray profiles = data.getJSONArray("profiles");
            int count = profiles.length();
            Status status = new Status();
            status.all = data.getInt("count");

            ProfileManager manager = ProfileManager.getInstance(context);

            for (int i = 0; i < count; i++) {
                try {
                    JSONObject profileData = profiles.getJSONObject(i);
                    String name = profileData.getString("name");
                    String content = profileData.getString("content");
                    ConfigParser cp = new ConfigParser();
                    cp.parseConfig(new StringReader(content));
                    VpnProfile profile = cp.convertProfile();
                    profile.mName = name;
                    manager.addProfile(profile);
                    manager.saveProfile(context, profile);
                    status.success++;
                } catch (JSONException | ConfigParser.ConfigParseError e) {
                    status.failed++;
                }
            }

            manager.saveProfileList(context);
            return status;

        } catch (IOException e) {
            throw new ProfileException(context.getString(R.string.profile_read_error));
        } catch (JSONException e) {
            throw new ProfileException(context.getString(R.string.profile_parse_error));
        }
    }

    public void removeAllProfile() {
        ProfileManager manager = ProfileManager.getInstance(context);
        LinkedList<VpnProfile> profiles = new LinkedList<>(manager.getProfiles());

        for (VpnProfile profile : profiles) {
            manager.removeProfile(context, profile);
        }
    }

    public static long bytesToLong(byte[] array) {
        return ((((long) array[0] & 0xff) << 56)
                | (((long) array[1] & 0xff) << 48)
                | (((long) array[2] & 0xff) << 40)
                | (((long) array[3] & 0xff) << 32)
                | (((long) array[4] & 0xff) << 24)
                | (((long) array[5] & 0xff) << 16)
                | (((long) array[6] & 0xff) << 8)
                | (((long) array[7] & 0xff) << 0));
    }

    protected boolean shouldUpdate() {
        long assetsTime = 0;
        long filesTime = 0;
        byte[] timeBytes = new byte[8];

        try {
            InputStream assetsProfile = context.getAssets().open(profileDataName);
            assetsProfile.read(timeBytes);
            assetsTime = bytesToLong(timeBytes);
        } catch (IOException e) {
            return false;
        }

        try {
            InputStream filesProfile = context.openFileInput(profileOutputName);
            filesProfile.read(timeBytes);
            filesTime = bytesToLong(timeBytes);
        } catch (IOException e) {
            return true;
        }

        return assetsTime > filesTime;
    }

    public void updateProfile() throws ProfileException {
        writeDefaultProfile();
        loadFromFile();
    }

    protected void writeDefaultProfile() throws ProfileException {
        try {
            InputStream inputStream = context.getAssets().open(profileDataName);
            OutputStream outputStream = context.openFileOutput(profileOutputName, Activity.MODE_PRIVATE);

            byte buf[] = new byte[4096];

            int lenread = inputStream.read(buf);

            while (lenread > 0) {
                outputStream.write(buf, 0, lenread);
                lenread = inputStream.read(buf);
            }

            outputStream.close();

        } catch (IOException e) {
            throw new ProfileException(context.getString(R.string.profile_cannot_write));
        }
    }

    public Status loadFromFile() throws ProfileException {
        try {
            removeAllProfile();
            InputStream inputStream = context.openFileInput(profileOutputName);
            inputStream.skip(8); //忽略开头的时间戳
            Reader fr = new InputStreamReader(new GZIPInputStream(inputStream), "UTF-8");
            return load(fr);
        } catch (IOException e) {
            throw new ProfileException(context.getString(R.string.profile_cannot_open));
        }
    }

    public class Status {
        public int success = 0;
        public int failed = 0;
        public int all = 0;
    }

    public static class ProfileException extends Exception {
        public ProfileException(String msg) {
            super(msg);
        }
    }
}
