package com.hnxy.hxy.app.flowstorm.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.hnxy.hxy.app.flowstorm.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView ver = (TextView) findViewById(R.id.version);
        String name = getString(R.string.app_name);
        String version = "";

        try {
            PackageInfo packageinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageinfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            //忽略异常
        }

        ver.setText(getString(R.string.version_info, name, version));

        WebView wv = (WebView)findViewById(R.id.webView);
        wv.loadUrl("file:///android_asset/full_licenses.html");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_return) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
