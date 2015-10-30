package com.example.triple_h.drivingbehavior;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {
    private WebView webview;
    LocationManager lm;
    private int TIME=1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);
        webview = new WebView(this);
        //设置WebView属性,能够执行JavaScript脚本
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebChromeClient(new WebChromeClient());
        try {
            //打开页面地址
            webview.loadUrl("http://192.168.1.194:8080/DrivingBehavior/");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

        });

        setContentView(webview);
        //获取系统LocationManager对象
        lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //从GPS获取最近的定位信息
        Location location=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateView(location);
        //设置每3秒获取一次GPS信息
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 8, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateView(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                updateView(null);
            }

            @Override
            public void onProviderEnabled(String provider) {
                updateView(lm.getLastKnownLocation(provider));
            }

            @Override
            public void onProviderDisabled(String provider) {
                updateView(lm.getLastKnownLocation(provider));
            }
        });
        handler.postDelayed(runnable, TIME); //每隔1s执行
    }
    Handler handler = new Handler();
    Runnable runnable = new Runnable()
    {
        @Override
        public void run() {
                // handler自带方法实现定时器
               try
               {
                   handler.postDelayed(this, TIME);
                   webview.loadUrl("javascript:theLocation(" + 106.29637269 + "," + 29.59953865 + ")");
                   System.out.println("do...");
                 }
               catch (Exception e) {
                 e.printStackTrace();
                 System.out.println("exception...");
              }
                }
          };

    public void updateView(Location newlocation)
    {
        if(newlocation!=null)
        {
            //webview.loadUrl("javascript:theLocation(" + newlocation.getLongitude() + "," + newlocation.getLatitude() + ")");
            //String a=String.valueOf(newlocation.getLongitude())+"  ,  "+newlocation.getLatitude();
            //Log.i("CDH", a+webview.getUrl());
            /*StringBuilder sb=new StringBuilder();
            sb.append("实时的位置信息： \n");
            sb.append("经度： ");
            sb.append(newlocation.getLongitude());
            sb.append("\n纬度： ");
            sb.append(newlocation.getLatitude());
            sb.append("\n高度： ");
            sb.append(newlocation.getAltitude());
            sb.append("\n速度： ");
            sb.append(newlocation.getSpeed());
            sb.append("\n方向： ");
            sb.append(newlocation.getBearing());
            Show.setText(sb.toString());*/

        }
        else
        {
            Log.i("CDH", "无法定位");
        }
    }
    @Override
    public boolean onKeyDown(int keyCoder, KeyEvent event) {
        if ((keyCoder == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack();
            return true;
        }
        return super.onKeyDown(keyCoder, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
