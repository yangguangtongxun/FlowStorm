package com.hnxy.hxy.app.flowstorm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hnxy.hxy.app.flowstorm.R;
import com.hnxy.hxy.app.flowstorm.adapter.ImagePagerAdapter;
import com.hnxy.hxy.app.flowstorm.autoscallviewpager.AutoScrollViewPager;
import com.hnxy.hxy.app.flowstorm.utils.Constants;
import com.hnxy.hxy.app.flowstorm.utils.Utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, VpnStatus.StateListener, VpnStatus.LogListener, VpnStatus.ByteCountListener {
    AutoScrollViewPager asvp_pager;
    ImageView iv_pager1, iv_pager2, iv_pager3;
    private static final int START_VPN_PROFILE = 70;
    private static final String TAG = MainActivity.class.getName();


    private enum BtnStatus {CONNECT, DISCONNECT}

    private static BtnStatus mBtnStatus = BtnStatus.CONNECT;
    private static VpnProfile mProfile;
    private static Account mAccount = new Account();
    private static LogRecoder mLogRecoder;
    private static boolean mDisconnected = false;
    private static int mConnectStatus = -1;

    //private boolean mCmfixed=false;

    private final Handler mStatusHandler = new StatusHandler(this);
    private final Handler mLogHandler = new LogHandler(this);
    private final Handler mByteHandler = new ByteHandler(this);

    protected OpenVPNService mService;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeDir();

        changeBtnStatus(mBtnStatus);
        VpnStatus.addStateListener(this);
        VpnStatus.addLogListener(this);
        VpnStatus.addByteCountListener(this);
        if (mLogRecoder == null) {
            try {
                mLogRecoder = new LogRecoder(this, Environment.getExternalStorageDirectory().getPath() +
                        getString(R.string.debug_log_file));
                VpnStatus.addLogListener(mLogRecoder);
            } catch (IOException e) {
                //ignore
            }
        }

        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //载入账号
        mAccount = AccountManager.getInstance(this).loadAccount();
        showAccount(mAccount);

        preferencesInit();
        loadProfile();
        mProfile = ProfileManager.getLastConnectedProfile(this, false);

        if (mProfile == null) {
            ProfileManager manager = ProfileManager.getInstance(this);
            ArrayList<VpnProfile> profiles = new ArrayList<>(manager.getProfiles());

            if (profiles.size() > 0) {
                Collections.sort(profiles);
                mProfile = profiles.iterator().next();
            }
        }

        showProfile(mProfile);
        initViews();
        checkVersion();
        //        checkNewVersion();
    }

    private void initViews() {//
        TextView tv_release = (TextView) findViewById(R.id.tvw_version);
        tv_release.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        tv_release.getPaint().setAntiAlias(true);//抗锯齿
        tv_release.setOnClickListener(this);
        iv_pager1 = (ImageView) findViewById(R.id.iv_pager1);
        iv_pager2 = (ImageView) findViewById(R.id.iv_pager2);
        iv_pager3 = (ImageView) findViewById(R.id.iv_pager3);
        asvp_pager = (AutoScrollViewPager) findViewById(R.id.asvp_pager);
        //        ViewGroup.LayoutParams params = asvp_pager.getLayoutParams();
        //        Display display = getWindowManager().getDefaultDisplay();
        //        params.width = display.getWidth();
        //        params.height = (int) (display.getWidth() * 1.3);
        //        asvp_pager.setLayoutParams(params);
        asvp_pager.setInterval(2000);
        asvp_pager.startAutoScroll();
        asvp_pager.setSlideBorderMode(AutoScrollViewPager.SLIDE_BORDER_MODE_TO_PARENT);
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, getResources().getStringArray(R.array.home_page_item_img_names));
        asvp_pager.setAdapter(adapter);
        asvp_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        iv_pager1.setImageResource(R.mipmap.pager_current);
                        iv_pager2.setImageResource(R.mipmap.pager_other);
                        iv_pager3.setImageResource(R.mipmap.pager_other);
                        break;
                    case 1:
                        iv_pager1.setImageResource(R.mipmap.pager_other);
                        iv_pager2.setImageResource(R.mipmap.pager_current);
                        iv_pager3.setImageResource(R.mipmap.pager_other);
                        break;
                    case 2:
                        iv_pager1.setImageResource(R.mipmap.pager_other);
                        iv_pager2.setImageResource(R.mipmap.pager_other);
                        iv_pager3.setImageResource(R.mipmap.pager_current);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvw_version:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_BASE + Constants.URL_WAP_APP_LIST));
                startActivity(intent);
                break;
        }
    }

    Handler udpateVersionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //            super.handleMessage(msg);
            if (msg.what == 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("发现新版本");
                final View layout = getLayoutInflater().inflate(R.layout.new_version, null, false);
                TextView version = (TextView) layout.findViewById(R.id.version);
                TextView introd = (TextView) layout.findViewById(R.id.introd);
                version.setText(getString(R.string.app_name) + "  " + Utils.getAppVersionName(MainActivity.this));
                introd.setText(getString(R.string.version_update_context));
                builder.setView(layout);
                builder.setPositiveButton("下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_BASE + Constants.URL_WAP_APP_LIST));
                        startActivity(intent);
                    }
                });

                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        }
    };

    class CheckVersionTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String filePaht = Constants.URL_BASE + Constants.URL_CHECK_VERSION;
                URL url = new URL(filePaht);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Charser", "GBK,utf-8;q=0.7,*;q=0.3");
                conn.setRequestProperty("Referer", url.toString());
                InputStream inputStream = conn.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                //                byte[] buffer = new byte[256];
                //                while (inputStream.read(buffer) > 0) {
                //                    outputStream.write(buffer, 0, buffer.length);
                //                }
                String version = "";

                while ((version = reader.readLine()) != null) {
                    if (version.contains(getString(R.string.version_name))) {
                        if (!version.contains(Utils.getAppVersionCode(MainActivity.this)+"")) {//有新版本
                            udpateVersionHandler.sendEmptyMessage(1);
                        }
                        break;
                    }
                }
                inputStream.close();
                conn.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 检测版本
     */
    private void checkVersion() {

        CheckVersionTask task = new CheckVersionTask();
        task.execute("");
        //        StringRequest request = new StringRequest(Constants.URL_BASE + Constants.URL_CHECK_VERSION, new Response.Listener<String>() {
        //            @Override
        //            public void onResponse(final String string) {
        //                System.out.println(string);
        //                try {
        //                    String netVersion = jsonObject.getString("version_number");
        //                    if (!netVersion.equals(Utils.getAppVersionName(MainActivity.this))) {
        //                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //                        builder.setTitle("发现新版本");
        //                        final View layout = getLayoutInflater().inflate(R.layout.new_version, null, false);
        //                        TextView version = (TextView) layout.findViewById(R.id.version);
        //                        TextView introd = (TextView) layout.findViewById(R.id.introd);
        //                        version.setText(getString(R.string.app_name) + "   " + Utils.getAppVersionName(MainActivity.this));
        //                        introd.setText(getString(R.string.version_update_context));
        //                        builder.setView(layout);
        //                        builder.setPositiveButton("下载", new DialogInterface.OnClickListener() {
        //                            @Override
        //                            public void onClick(DialogInterface dialog, int which) {
        //                                dialog.dismiss();
        //                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_BASE + Constants.URL_WAP_APP_LIST));
        //                                startActivity(intent);
        //                            }
        //                        });
        //
        //                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
        //                            @Override
        //                            public void onClick(DialogInterface dialog, int which) {
        //                                dialog.dismiss();
        //                            }
        //                        });
        //                        builder.create().show();
        //                    }
        //                } catch (JSONException e) {
        //                    e.printStackTrace();
        //                }
        //            }
        //        }, new Response.ErrorListener() {
        //            @Override
        //            public void onErrorResponse(VolleyError volleyError) {
        //                Utils.showToast(MainActivity.this, "版本更新检测失败");
        //            }
        //        });
        //        MyApplication.getInstance().addToRequestQueue(request, TAG);
    }

    private void makeDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/" + Constants.DIR_BASE + "/" + Constants.DIR_IMG);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VpnStatus.removeLogListener(this);
        VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);
    }

    protected void loadProfile() {
        ProfileLoader loader = new ProfileLoader(this);

        try {
            if (loader.shouldUpdate()) {
                loader.updateProfile();
            }
        } catch (ProfileLoader.ProfileException e) {
            setStatus(e.getMessage());
        }
    }

    protected void preferencesInit() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("usesystemproxy", false);
        editor.putBoolean("netchangereconnect", false);
        editor.putBoolean("ovpn3", false);
        editor.putBoolean("restartvpnonboot", false);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    public void showAbout(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void btnAccountOnClick(View view) {
        askForPW(R.string.password, false);
    }

    public void btnConnectOnClick(View view) {
        switch (mBtnStatus) {
            case CONNECT:
                connect();
                break;
            case DISCONNECT:
                disconnect();
                break;
        }
    }

    public void changeBtnStatus(BtnStatus stat) {
        Button btnConnect = (Button) findViewById(R.id.btn_connect);

        switch (stat) {
            case CONNECT:
                mBtnStatus = BtnStatus.CONNECT;
                btnConnect.setText("连  接");
                btnConnect.setTextColor(Color.WHITE);
                //                btnConnect.setImageResource(R.drawable.btn_connect);
                break;
            case DISCONNECT:
                mBtnStatus = BtnStatus.DISCONNECT;
                btnConnect.setText("断  开");
                btnConnect.setTextColor(Color.RED);
                //                btnConnect.setImageResource(R.drawable.btn_disconnect);
                break;
        }
    }

    public void disconnect() {
        if (mService != null && mService.getManagement() != null) {
            mService.getManagement().stopVPN();
        }

        //ProfileManager.setConntectedVpnProfileDisconnected(this);
    }

    public void connect() {

        try {
            changeBtnStatus(BtnStatus.DISCONNECT);
            mDisconnected = false;
            showAccount(mAccount);
            showProfile(mProfile);
            setStatus(getString(R.string.state_prepare));

            if (mProfile == null) {
                setStatus(getString(R.string.state_profile_nochoose));
                selectProfile(true);
                changeBtnStatus(BtnStatus.CONNECT);
                return;
            }

            int vpnok = mProfile.checkProfile(this);

            if (vpnok != R.string.no_error_found) {
                throw new ProfileLoader.ProfileException(getString(R.string.state_profile_error));
            }

            mProfile.mConnectRetryMax = "-1";
            mProfile.mConnectRetry = "0";
            mProfile.mPersistTun = true;
            mAccount.updateProfile(mProfile);

            Intent intent = VpnService.prepare(this);

            /*
            // Check if we want to fix /dev/tun
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean usecm9fix = prefs.getBoolean("useCM9Fix", false);
            boolean loadTunModule = prefs.getBoolean("loadTunModule", false);

            if(loadTunModule)
                executeSUcmd("insmod /system/lib/modules/tun.ko");

            if(usecm9fix && !mCmfixed ) {
                executeSUcmd("chown system /dev/tun");
            }*/

            if (intent != null) {
                VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                        VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
                // Start the query
                try {
                    startActivityForResult(intent, START_VPN_PROFILE);
                } catch (ActivityNotFoundException ane) {
                    // Shame on you Sony! At least one user reported that
                    // an official Sony Xperia Arc S image triggers this exception
                    VpnStatus.logError(R.string.no_vpn_support_image);
                    //showLogWindow();
                }
            } else {
                onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
            }

        } catch (ProfileLoader.ProfileException e) {
            setStatus(e.getMessage());
            changeBtnStatus(BtnStatus.CONNECT);
        }
    }

    public void btnProfileOnClick(View view) {
        selectProfile(false);
    }

    private void selectProfile(final boolean recall) {
        final View profileLayout = getLayoutInflater().inflate(R.layout.profile_list, null, false);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getString(R.string.choose_profile));
        dialogBuilder.setView(profileLayout);
        final AlertDialog dialog = dialogBuilder.create();
        ListView profileList = (ListView) profileLayout.findViewById(R.id.profileList);
        profileList.setAdapter(new ListAdapter() {
            ArrayList<VpnProfile> profileList;

            //初始化块
            {
                profileList = new ArrayList<VpnProfile>(ProfileManager.getInstance(getBaseContext()).getProfiles());
                Collections.sort(profileList);
            }

            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return profileList.size();
            }

            @Override
            public Object getItem(int position) {
                return profileList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    TextView view = (TextView) getLayoutInflater().inflate(R.layout.profile_list_element, parent, false);
                    view.setText(profileList.get(position).getName());
                    return view;
                } else {
                    ((TextView) convertView).setText(profileList.get(position).getName());
                    return convertView;
                }
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
        profileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mProfile = (VpnProfile) parent.getItemAtPosition(position);
                showProfile(mProfile);
                ProfileManager.setConnectedVpnProfile(getBaseContext(), mProfile);
                dialog.dismiss();

                if (recall) {
                    btnConnectOnClick(view);
                }
            }
        });
        dialog.show();
    }

    private void askForPW(final int type, final boolean recall) {

        final EditText entry = new EditText(this);
        final View userpwlayout = getLayoutInflater().inflate(R.layout.userpass, null, false);

        entry.setSingleLine();
        entry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        entry.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        if (recall) {
            dialog.setTitle(getString(R.string.need_password, getString(type)));
        } else {
            dialog.setTitle(R.string.setting_account);
        }
        //dialog.setMessage(R.string.enter_password);

        if (type == R.string.password) {
            ((EditText) userpwlayout.findViewById(R.id.username)).setText(mAccount.mUsername);
            ((EditText) userpwlayout.findViewById(R.id.password)).setText(mAccount.mPassword);
            ((CheckBox) userpwlayout.findViewById(R.id.save_password)).setChecked(!TextUtils.isEmpty(mAccount.mPassword));
            ((CheckBox) userpwlayout.findViewById(R.id.show_password)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)
                        ((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    else
                        ((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            });

            dialog.setView(userpwlayout);
        } else {
            dialog.setView(entry);
        }

        AlertDialog.Builder builder = dialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (type == R.string.password) {
                            mAccount.mUsername = ((EditText) userpwlayout.findViewById(R.id.username)).getText().toString();

                            String pw = ((EditText) userpwlayout.findViewById(R.id.password)).getText().toString();
                            if (((CheckBox) userpwlayout.findViewById(R.id.save_password)).isChecked()) {
                                mAccount.mPassword = pw;
                                mAccount.mTransientPW = null;
                            } else {
                                mAccount.mPassword = null;
                                mAccount.mTransientPW = pw;
                            }
                        } else {
                            mAccount.mTransientPCKS12PW = entry.getText().toString();
                        }

                        if (recall) {
                            mAccount.updateProfile(mProfile);
                            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
                        } else {
                            //setStatus(getString(R.string.account_saved));
                        }

                        showAccount(mAccount);

                        try {
                            AccountManager.getInstance(getBaseContext()).saveAccount(mAccount);
                        } catch (AccountManager.AccountException e) {
                            setStatus(e.getMessage());
                        }
                    }

                });
        dialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (recall) {
                            VpnStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
                                    VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED);
                            changeBtnStatus(BtnStatus.CONNECT);
                        } else {
                            //setStatus(getString(R.string.account_not_save));
                        }

                        showAccount(mAccount);
                    }
                });

        dialog.create().show();

    }

    protected void showProfile(VpnProfile profile) {
        TextView view = (TextView) findViewById(R.id.tvw_profile);

        if (profile != null && profile.mName != null) {
            view.setText(profile.mName);
        } else {
            view.setText(getString(R.string.profilestate_nochoose));
        }
    }

    protected void showAccountCounter(final Account account) {
        final AccountCounterHandler handler = new AccountCounterHandler(this);

        new Thread() {
            @Override
            public void run() {
                ApiClient api = new ApiClient(getBaseContext());
                try {
                    ApiClient.AccountCounter counter = api.accountCount(account);
                    Message message = handler.obtainMessage();
                    message.obj = counter;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Message message = handler.obtainMessage();
                    message.obj = e;
                    handler.sendMessage(message);
                }
            }
        }.start();
    }

    private static class AccountCounterHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        AccountCounterHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (ApiClient.ApiException.class.equals(msg.obj.getClass())) {
                ApiClient.ApiException e = (ApiClient.ApiException) msg.obj;
                mActivity.get().setCounterStatus("应用内部错误:", e.getMessage());

            } else if (ApiClient.AccountCounter.class.equals(msg.obj.getClass())) {
                ApiClient.AccountCounter counter = (ApiClient.AccountCounter) msg.obj;

                if (counter.error) {
                    mActivity.get().setCounterStatus("帐号状况异常:", counter.errMsg);
                } else {
                    String left = "";
                    String right = "";

                    if (counter.maxGlobalTraffic != 0) {
                        left += "已用流量: " + OpenVPNService.humanReadableByteCount(counter.globalTrafficCount, false);
                        right += "剩余流量: " + OpenVPNService.humanReadableByteCount(counter.maxGlobalTraffic - counter.globalTrafficCount, false);
                    }

                    if (counter.maxActiveDays != 0) {
                        if (counter.maxGlobalTraffic != 0) {
                            left += "\n";
                            right += "\n";
                        }

                        left += "已用天数: " + counter.activeDaysCount + " 天";
                        right += "剩余天数: " + (counter.maxActiveDays - counter.activeDaysCount) + " 天";
                    }

                    mActivity.get().setCounterStatus(left, right);
                }
            }
        }
    }

    protected void checkNewVersion() {
        final NewVersionHandler handler = new NewVersionHandler(this);

        new Thread() {
            @Override
            public void run() {
                ApiClient api = new ApiClient(getBaseContext());
                try {
                    ApiClient.NewVersion newVersion = api.checkNewVersion();
                    Message message = handler.obtainMessage();
                    message.obj = newVersion;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Message message = handler.obtainMessage();
                    message.obj = e;
                    handler.sendMessage(message);
                }
            }
        }.start();
    }

    private static class NewVersionHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        NewVersionHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (ApiClient.ApiException.class.equals(msg.obj.getClass())) {
                // ignore
            } else if (ApiClient.NewVersion.class.equals(msg.obj.getClass())) {
                final ApiClient.NewVersion newVersion = (ApiClient.NewVersion) msg.obj;

                if (newVersion.error) {
                    // ignore
                } else if (!newVersion.newVersion) {
                    // ignore
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity.get());
                    builder.setTitle("发现新版本");
                    final View layout = mActivity.get().getLayoutInflater().inflate(R.layout.new_version, null, false);
                    TextView version = (TextView) layout.findViewById(R.id.version);
                    TextView introd = (TextView) layout.findViewById(R.id.introd);
                    version.setText(newVersion.name + " v" + newVersion.version + " (" + newVersion.getSize() + ")");
                    introd.setText(newVersion.introd);
                    builder.setView(layout);

                    builder.setPositiveButton("下载", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newVersion.url));
                            mActivity.get().startActivity(intent);
                        }
                    });

                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            }
        }
    }

    public void setCounterStatus(String left, String right) {
        TextView tvwLeft = (TextView) findViewById(R.id.tvw_counter_left);
        TextView tvwRight = (TextView) findViewById(R.id.tvw_counter_right);
        tvwLeft.setText(left);
        tvwRight.setText(right);
    }

    protected void showAccount(Account account) {
        TextView view = (TextView) findViewById(R.id.tvw_account);

        if (account.mUsername != null && !"".equals(account.mUsername)) {
            view.setText(account.mUsername);
            showAccountCounter(account);
        } else {
            view.setText(getString(R.string.accountstate_nochoose));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
                int needpw = mProfile.needUserPWInput(false);
                if (needpw != 0) {
                    VpnStatus.updateStateString("USER_VPN_PASSWORD", "", R.string.state_user_vpn_password,
                            VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
                    //setStatus(getString(R.string.state_need_account));
                    askForPW(needpw, true);
                } else {
                    setStatus(getString(R.string.state_start_vpn));
                    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    new startOpenVpnThread().start();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User does not want us to start, so we just vanish
                /*VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
                        VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED);*/
                setStatus(getString(R.string.state_user_vpn_permission_cancelled));
                changeBtnStatus(BtnStatus.CONNECT);
            }
        }
    }

    /*private void executeSUcmd(String command) {
        ProcessBuilder pb = new ProcessBuilder("su","-c",command);
        try {
            Process p = pb.start();
            int ret = p.waitFor();
            if(ret ==0)
                mCmfixed=true;
        } catch (InterruptedException | IOException e) {
            VpnStatus.logException("SU command", e);

        }
    }*/

    public void setStatus(String msg) {
        TextView status = (TextView) findViewById(R.id.tvw_status);
        status.setText(msg);
    }

    /*public void setUpload(long diffOut, long out) {
        TextView upload = (TextView) findViewById(R.id.tvw_upload);
        upload.setText(getString(R.string.statusline_upload,
                OpenVPNService.humanReadableByteCount(out, false),
                OpenVPNService.humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, false)));
    }

    public void setDownload(long diffIn, long in) {
        TextView download = (TextView) findViewById(R.id.tvw_download);
        download.setText(getString(R.string.statusline_download,
                OpenVPNService.humanReadableByteCount(in, false),
                OpenVPNService.humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, false)));
    }*/


    @Override
    public void updateState(String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("state", state);
        bundle.putString("logmessage", logmessage);
        bundle.putInt("localizedResId", localizedResId);
        bundle.putInt("level", level.ordinal());
        message.setData(bundle);
        mStatusHandler.sendMessage(message);
    }

    @Override
    public void newLog(VpnStatus.LogItem logItem) {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putLong("time", logItem.getLogtime());
        bundle.putString("message", logItem.getString(this));
        bundle.putInt("level", logItem.getVerbosityLevel());
        message.setData(bundle);
        mLogHandler.sendMessage(message);

        //Log.d("Log/" + logItem.getLogLevel().toString(), logItem.getString(this));
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putLong("in", in);
        bundle.putLong("out", out);
        bundle.putLong("diffIn", diffIn);
        bundle.putLong("diffOut", diffOut);
        message.setData(bundle);
        mByteHandler.sendMessage(message);
    }

    private class startOpenVpnThread extends Thread {

        @Override
        public void run() {
            VPNLaunchHelper.startOpenVpn(mProfile, getBaseContext());
        }

    }

    private static class StatusHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        StatusHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            int localizedResId = bundle.getInt("localizedResId");
            String state = bundle.getString("state");
            boolean showStatus = true;
            int level = bundle.getInt("level");

            if (level == VpnStatus.ConnectionStatus.LEVEL_AUTH_FAILED.ordinal() ||
                    level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED.ordinal()) {
                showStatus = !mDisconnected;
                mDisconnected = true;
            }

            if (showStatus) {
                mActivity.get().setStatus(mActivity.get().getString(localizedResId));
            }

            if ("NOPROCESS".equals(state) && mBtnStatus == BtnStatus.DISCONNECT) {
                mActivity.get().changeBtnStatus(BtnStatus.CONNECT);
            }

            MainActivity.mConnectStatus = level;
        }
    }

    private static class LogHandler extends Handler {
        WeakReference<MainActivity> mActivity;
        String mHttpProxyReturned = null;

        LogHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String message = bundle.getString("message");
            int level = bundle.getInt("level");

            if (message.startsWith("HTTP proxy returned:")) {
                mHttpProxyReturned = message.substring(21);

            } else if (message.startsWith("HTTP proxy returned bad status")) {
                String state = mActivity.get().getString(R.string.http_proxy_error);

                if (mHttpProxyReturned != null) {
                    state += mHttpProxyReturned;
                }

                mActivity.get().setStatus(state);
                mActivity.get().mDisconnected = true;

            } else if (message.startsWith("Options error:")) {
                mActivity.get().setStatus(mActivity.get().getString(R.string.state_profile_error));
                mActivity.get().mDisconnected = true;

            }

        }
    }

    private static class ByteHandler extends Handler {
        WeakReference<MainActivity> mActivity;
        int counter = 0;

        ByteHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mConnectStatus == VpnStatus.ConnectionStatus.LEVEL_CONNECTED.ordinal()) {
                counter++;

                if (counter > 100) {
                    counter = 0;
                    mActivity.get().showAccountCounter(mAccount);
                }
            }
        }
    }
}
