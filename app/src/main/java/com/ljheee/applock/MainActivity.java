package com.ljheee.applock;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


public class MainActivity extends ActionBarActivity {


    private Button button ;

    //第一次打开“有权查看使用情况的应用”
    private boolean isFirst = true;
    ArrayList<String> forbidApps = new ArrayList<>();


    Handler  handler = new Handler(){

        public void handleMessage(android.os.Message msg){

            //getTaskPackname();
//            if("com.ljheee.jokes".equals(getTaskPackname())){
            if(forbidApps.contains(getTaskPackname())){
                startActivity(new Intent(MainActivity.this,LockActivity.class));
            }

            handler.sendEmptyMessageDelayed(1, 500);//500毫秒“轮询”一次，查看栈顶app
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button)findViewById(R.id.button);

        forbidApps.add("com.ljheee.jokes");
        forbidApps.add("com.ljheee.footernavigation");

        handler.sendEmptyMessageDelayed(1, 500);
        //isNoSwitch();

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                goToAndroidSettings();
                //使用UsageStatsManager获取，但是这种获取方法需要用户在手机上赋予APP权限才可以使用，就是在安全-高级-有权查看使用情况的应用 在这个模块中勾选上指定APP就可以获取到栈顶的应用名
            }
        });
    }

    /**
     * 打开“有权查看使用情况的应用”
     * Android 5.0以后版本
     */
    public void goToAndroidSettings(){
        //打开--com.android.settings
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    /**
     * 获取栈顶应用程序--包名
     * @return
     */
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @SuppressLint("NewApi")
    private String getTaskPackname() {
        boolean isNoSwitch = isNoSwitch();
        Log.e("LJHEEE", "isNoSwitch=: " + isNoSwitch);

        String currentApp = "CurrentNULL";
        //LOLLIPOP = API 21,Android 5.0
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);


            //5.1以上，如果不打开此设置，queryUsageStats获取到的是size为0的list
//            if(isFirst||(!isNoSwitch())||appList.size() == 0 || appList == null){
            if((!isNoSwitch())&&isFirst){
                isFirst = false;
                goToAndroidSettings();
            }else{
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }
        Log.e("TAG", "Current App in foreground is: " + currentApp);
        return currentApp;
    }

    /**
     * 判断当前设备中有没有 “有权查看使用权限的应用” 这个选项
     * @return
     */
    private boolean isNoOption() {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * 判断调用该设备中“有权查看使用权限的应用”这个选项的APP有没有打开
     * @return  此APP打开这个选项，就返回true
     */
    private boolean isNoSwitch() {
        long ts = System.currentTimeMillis();
        UsageStatsManager usageStatsManager = (UsageStatsManager) getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, 0, ts);
        if (queryUsageStats == null || queryUsageStats.isEmpty()) {
            return false;
        }
        return true;
    }

}
