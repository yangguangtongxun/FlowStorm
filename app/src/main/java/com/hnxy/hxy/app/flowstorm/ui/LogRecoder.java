package com.hnxy.hxy.app.flowstorm.ui;

import android.content.Context;

import com.hnxy.hxy.app.flowstorm.R;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by hu60 on 2015-08-21.
 */
public class LogRecoder implements VpnStatus.LogListener {
    private FileWriter logWriter;
    private Context context;

    public LogRecoder(Context context, String path) throws IOException {
        this.context = context;
        logWriter = new FileWriter(path);
    }

    @Override
    public void newLog(VpnStatus.LogItem logItem) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.log_time_format));
            Date logTime = new Date(logItem.getLogtime());
            logWriter.write(sdf.format(logTime));
            logWriter.write(logItem.getString(context));
            logWriter.write("\n");
            logWriter.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    public void finalize() {
        try {
            logWriter.close();
        } catch (IOException e) {
            //ignore
        }
    }
}
