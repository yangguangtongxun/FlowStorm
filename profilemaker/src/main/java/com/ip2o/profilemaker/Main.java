package com.ip2o.profilemaker;

import java.lang.String;
import java.lang.System;

public class Main {

    public static void main(String args[]) {
        System.out.println("流量大师配置文件打包工具");

        if (args.length < 2) {
            System.out.println("用法：");
            System.out.println("    java com.ip2o.profilemaker.Main profiles_path output_path");
            return;
        }

        String profiles_path = args[0];
        String output_path = args[1];
        String base_dir = "";

        if (args.length >= 3) {
            base_dir = args[2];
        }

        ProfileMaker profileMaker = new ProfileMaker(base_dir);
        profileMaker.addDirectory(profiles_path);
        profileMaker.writeToFile(output_path + "/profile.dat");
    }
}