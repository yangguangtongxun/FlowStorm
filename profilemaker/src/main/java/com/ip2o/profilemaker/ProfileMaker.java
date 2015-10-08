package com.ip2o.profilemaker;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.String;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;


/**
 * 配置文件生成器
 *
 * 配置文件Json数据格式：
 *
 * {
 *     "version": 1
 *     "count": 数量,
 *     "profiles": [
 *         {
 *             "name": 配置文件名,
 *             "content": 配置文件内容
 *         },
 *         ...
 *     ]
 * }
 *
 * 配置文件存储格式：
 *     前8字节：long型，配置文件生成时的系统时间毫秒数（System.currentTimeMillis()的返回值）
 *     后续字节：gzip压缩的JSON字符串数据
 *
 */
public class ProfileMaker {
    protected final int mVersion = 1;
    protected int mCount = 0;
    protected JSONArray mProfiles = new JSONArray();
    protected String mBaseDir = "";

    public ProfileMaker(String baseDir) {
        mBaseDir = baseDir + "/";
    }

    public void addProfile(String name, String content) {
        JSONObject profile = new JSONObject();
        profile.put("name", name);
        profile.put("content", content);
        mProfiles.put(profile);
        mCount ++;
    }

    public void addFile(String path) {
        System.out.println("添加文件：" + removeBaseDir(path));

        try {
            File file = new File(path);
            String name = file.getName();
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            name = name.substring(0, name.lastIndexOf('.'));

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            addProfile(name, sb.toString());

        } catch (IOException e) {
            System.out.println("添加文件出错，该文件被忽略");
        }
    }

    public void addDirectory(String path) {
        System.out.println("添加目录：" + removeBaseDir(path) + "/");

        File file = new File(path);

        if (file == null || !file.exists()) {
            try {
                Files.createDirectories(Paths.get(path));
                System.out.println("目录不存在，已自动创建");
                System.out.println("请将配置文件放在 " + removeBaseDir(path) + "/ 目录中");
            } catch (IOException e) {
                System.out.println("目录创建失败");
                System.out.println("请手动创建 " + removeBaseDir(path) + "/ 目录");
            }

            return;
        }

        String [] profiles = file.list();
        Arrays.sort(profiles, String.CASE_INSENSITIVE_ORDER);

        for (String profile: profiles) {
            if (profile.matches("^.*\\.ovpn$")) {
                addFile(path + "/" + profile);
            }
        }
    }

    public static byte[] longToBytes(long n) {
        byte[] b = new byte[8];

        b[7] = (byte) (n & 0xff);
        b[6] = (byte) (n >> 8  & 0xff);
        b[5] = (byte) (n >> 16 & 0xff);
        b[4] = (byte) (n >> 24 & 0xff);
        b[3] = (byte) (n >> 32 & 0xff);
        b[2] = (byte) (n >> 40 & 0xff);
        b[1] = (byte) (n >> 48 & 0xff);
        b[0] = (byte) (n >> 56 & 0xff);

        return b;
    }

    public void writeToFile(String path) {

        if (mCount < 1) {
            System.out.println("没有找到任何配置文件，请先添加配置文件");
            return;
        }

        try {
            Path dirPath = Paths.get(path);
            dirPath = dirPath.getParent();
            File dir = new File(dirPath.toString());

            if (dir == null || !dir.exists()) {
                Files.createDirectories(dirPath);
                System.out.println("目录 " + dirPath.toString() + "已自动创建");
            }

            FileOutputStream fileOutputStream = new FileOutputStream(path);
            Long time = System.currentTimeMillis();
            byte[] timeBytes = longToBytes(time);
            fileOutputStream.write(timeBytes);

            Writer writer = new OutputStreamWriter(new GZIPOutputStream(fileOutputStream), "UTF-8");
            JSONWriter jsonWriter = new JSONWriter(writer);

            jsonWriter.object().key("version").value(mVersion).
                    key("count").value(mCount).
                    key("profiles").value(mProfiles).endObject();

            writer.close();
            fileOutputStream.close();
            System.out.println("配置文件已打包为 " + path);

        } catch (Exception e) {
            System.out.println("打包配置文件出错：");
            e.printStackTrace();
        }

    }

    protected String removeBaseDir(String path) {
        return path.substring(mBaseDir.length());
    }
}