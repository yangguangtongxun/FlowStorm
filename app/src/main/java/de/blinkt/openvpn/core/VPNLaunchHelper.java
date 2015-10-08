/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hnxy.hxy.app.flowstorm.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;

public class VPNLaunchHelper {
    private static final String MININONPIEVPN = "nopie_openvpn";
    private static final String MINIPIEVPN = "pie_openvpn";
    //private static final String OVPNCONFIGFILE = "android.conf";
    private static VpnProfile mStartProfile = null;
    private static Context mStartContext = null;

    static private String writeMiniVPN(Context context) {
        String[] abis = getSupportedAbis();

        for (String abi: abis) {

            File mvpnout = new File(context.getCacheDir(), getMiniVPNExecutableName() + "." + abi);
            if ((mvpnout.exists() && mvpnout.canExecute()) || writeMiniVPNBinary(context, abi, mvpnout)) {
                return mvpnout.getPath();
            }
        }

        return null;
	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String[] getSupportedAbis() {
        String[] abis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            abis = Build.SUPPORTED_ABIS;
        else
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};

        return abis;
    }

    private static String getMiniVPNExecutableName()
    {
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.JELLY_BEAN)
            return MINIPIEVPN;
        else
            return MININONPIEVPN;
    }


    public static String[] replacePieWithNoPie(String[] mArgv)
    {
        mArgv[0] = mArgv[0].replace(MINIPIEVPN, MININONPIEVPN);
        return mArgv;
    }


    public static String[] buildOpenvpnArgv(Context c) {
        Vector<String> args = new Vector<>();

        // Add fixed paramenters
        //args.add("/data/data/de.blinkt.openvpn/lib/openvpn");
        args.add(writeMiniVPN(c));

        args.add("--config");
        args.add(/*getConfigFilePath(c)*/ "stdin");

        return args.toArray(new String[args.size()]);
    }

    private static boolean writeMiniVPNBinary(Context context, String abi, File mvpnout) {
        try {
            InputStream mvpn;

            try {
                mvpn = context.getAssets().open(getMiniVPNExecutableName() + "." + abi);
            }
            catch (IOException errabi) {
                VpnStatus.logInfo("Failed getting assets for archicture " + abi);
                return false;
            }


            FileOutputStream fout = new FileOutputStream(mvpnout);

            byte buf[]= new byte[4096];

            int lenread = mvpn.read(buf);
            while(lenread> 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            fout.close();

            if(!mvpnout.setExecutable(true)) {
                VpnStatus.logError("Failed to make OpenVPN executable");
                return false;
            }


            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
            return false;
        }

    }
	

	public static void startOpenVpn(VpnProfile startprofile, Context context) {
		if(writeMiniVPN(context)==null) {
			VpnStatus.logError(context.getString(R.string.unsupported_abi, getSupportedAbis()[0], context.getString(R.string.app_name)));
			return;
		}

		VpnStatus.logInfo(R.string.building_configration);

		Intent startVPN = startprofile.prepareStartService(context);
        mStartProfile = startprofile;
        mStartContext = context;

		if(startVPN!=null)
			context.startService(startVPN);

	}

    public static VpnProfile getStartProfile() {
        return mStartProfile;
    }

    public static Context getStartContext() {
        return mStartContext;
    }

    /*public static String getConfigFilePath(Context context) {
        return context.getCacheDir().getAbsolutePath() + "/" + OVPNCONFIGFILE;
    }*/

}
