package com.example.triple_h.drivingbehavior;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WebView webview;
    private int TIME=1000;
    public String BaseUrl="http://3040278.nat123.net:20306/DrivingBehavior";
    //声明Baidu定位
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    //声明定时器的Handler
    Handler handler = new Handler();
    //Latitude纬度
    double CurLatitude;
    //Lontitude经度
    double CurLontitude;
    //方向
    float orientation;
    //速度
    float speed;
    //方向
    float driection;
    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;
    //方向传感器
    private SensorManager sm;
    //需要一个Sensor
    private Sensor OSensor;
    //需要一个数组
    float[] orientationFieldValues = new float[3];
    private float lastZ;
    private long exitTime = 0;
    private static final String TAG = "orientationsensor";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);
        /*
        *创建Baidu定位
        */
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);    //注册监听函数
        initLocation();
        mLocationClient.start();
        //创建WebView
        webview = new WebView(this);
        //设置WebView属性,能够执行JavaScript脚本
        webview.getSettings().setJavaScriptEnabled(true);
        //设置网页的标题为标题栏的标题
        WebChromeClient wvcc = new WebChromeClient() {
                      @Override
                        public void onReceivedTitle(WebView view, String title) {
                             super.onReceivedTitle(view, title);
                           Log.d("ANDROID_LAB", "TITLE=" + title);
                          MainActivity.this.setTitle(title);
                        }
            @Override
            public boolean onJsAlert(WebView view, String url,
                                     String message, final JsResult result) {
                //用Android组件替换
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提示")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .create().show();
                return true;
            }
        };
        //应用webview的设置
        webview.setWebChromeClient(wvcc);
        //打开网页
        try {
            //打开页面地址
            webview.loadUrl(BaseUrl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //允许网页重定向
        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                if (url.startsWith(BaseUrl + "/demo/hello")) {

                }

            }
        });
        setContentView(webview);
        powerManager = (PowerManager)this.getSystemService(this.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Lock");

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        OSensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        //更新显示数据的方法
        calculateOrientation();

        handler.postDelayed(runnable, TIME); //每隔1s执行
    }

    Runnable runnable = new Runnable()
    {
        @Override
        public void run() {
            // handler自带方法实现定时器
            try
            {
                //handler.postDelayed(this, TIME);
                webview.loadUrl("javascript:SetSpeedAndDirection(" + speed + "," + driection + ")");
                webview.loadUrl("javascript:theLocation(" + CurLontitude + "," + CurLatitude + ","+orientation+")");

                Log.i("BaiduLocationCall", "成功调用"+CurLatitude+",  "+CurLontitude+" , "+orientation);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("exception...");
            }
            handler.postDelayed(this, TIME);
        }
    };
    @Override
    public boolean onKeyDown(int keyCoder, KeyEvent event) {
        if ((keyCoder == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            if(webview.getUrl().startsWith(BaseUrl + "/demo/hello")&&!webview.getUrl().contains("page_information_child")||webview.getUrl().equals(BaseUrl+"/"))
            {
                exit();
            }
            else
            {
                webview.goBack();
            }
            return true;
        }
        return super.onKeyDown(keyCoder, event);
    }
    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000)
        {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        }
        else
        {
            finish();
            System.exit(0);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                webview.reload();
                return true;
            case R.id.action_end:
                    handler.removeCallbacks(runnable);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span=1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location
            StringBuilder sb = new StringBuilder(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            CurLatitude=location.getLatitude();
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            CurLontitude=location.getLongitude();
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                speed=location.getSpeed();
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                driection=location.getDirection();
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list =location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            Log.i("BaiduLocationApiDem", sb.toString());
        }
    }
    final SensorEventListener SensormyListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION)
                orientationFieldValues=sensorEvent.values;
            calculateOrientation();

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    };
    private  void calculateOrientation() {
        float[] values = new float[3];
        float x;
        values=orientationFieldValues;
        Log.i(TAG, values[0]+"");
        x=values[0];
        if( Math.abs(x- lastZ) > 5.0 )
        {
            orientation=x;
        }
        lastZ = x ;
    }

    @Override
    protected void onResume()
    {
        sm.registerListener(SensormyListener, OSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        wakeLock.acquire();
     }

    @Override
    protected void onPause()
    {
        super.onPause();
        sm.unregisterListener(SensormyListener);
        wakeLock.release();
    }

}
