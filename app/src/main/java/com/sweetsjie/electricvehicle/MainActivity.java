package com.sweetsjie.electricvehicle;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private double latitude, longitude;
    private String power = "0";
    private String speed = "0";
    public static final int GET_INFORMATION = 0;
    private boolean isCut = false;

    private MapView mapview = null;
    private AMap aMap;
    //设置图钉选项
    private MarkerOptions options = new MarkerOptions();

    private TextView powerShow;
    private TextView speedShow;
    private EditText inputPhoneNumber;
    private Button cutPowerButton;

    PendingIntent sentPI, deliverPI;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //地图控件获取
        mapview = (MapView) findViewById(R.id.map_view);
        powerShow = (TextView) findViewById(R.id.powerShow);
        speedShow = (TextView) findViewById(R.id.speedShow);
        inputPhoneNumber = (EditText) findViewById(R.id.inputPhoneNumber);
        cutPowerButton = (Button) findViewById(R.id.cutPowerButton);


        getInformation();

        //地图定位
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，实现地图生命周期管理
        mapview.onCreate(savedInstanceState);
        //mapInit();


        cutPowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputPhoneNumber.getText().toString() == null || inputPhoneNumber.getText().toString().length()<11) {
                    Toast.makeText(MainActivity.this, "请输入正确手机号码", Toast.LENGTH_SHORT).show();
                } else {
                    if (isCut) {
                        cutPowerButton.setText("切断电源");
                        sendSMS(inputPhoneNumber.getText().toString(), "openpower");
                        Toast.makeText(MainActivity.this, "电源已开启", Toast.LENGTH_SHORT).show();
                        isCut = false;
                    } else {
                        cutPowerButton.setText("打开电源");
                        sendSMS(inputPhoneNumber.getText().toString(), "cutpower");
                        Toast.makeText(MainActivity.this, "电源已切断", Toast.LENGTH_SHORT).show();
                        isCut = true;
                    }
                }
            }
        });


    }

    @Override
    protected void onResume() {
        getInformation();
        mapInit();
        super.onResume();
    }

    private double dealPosition(double Position) {
        double Positon_Degree, Position_Cent, Positon_Sec, Position_buf;
        Positon_Degree = (int) (Position / 100);
        Position = Position - Positon_Degree * 100;
        Position_Cent = (int) Position;
        Positon_Sec = Position - Position_Cent;
        Position_buf = (double) (Positon_Degree + (Position_Cent / 60) + (Positon_Sec / 60));
        String buf = Double.toString(Position_buf);
        return (Positon_Degree + (Position_Cent / 60) + (Positon_Sec / 60));
    }

    //WebService线程
    private void getInformation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //组装反向地理编码的接口地址
                        StringBuilder url = new StringBuilder();
                        url.append("http://www.makercorner.cn:8080/SSMServer/user/read=1");
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpGet httpGet = new HttpGet(url.toString());
                        HttpResponse httpResponse = httpClient.execute(httpGet);
                        HttpEntity entity = httpResponse.getEntity();

                        String response = EntityUtils.toString(entity);
                        //response = response.substring(6);

                        JSONObject jsonObject = new JSONObject(response);

                        speed = jsonObject.getString("speed");
                        power = jsonObject.getString("power");
                        latitude = Double.parseDouble(jsonObject.getString("latitude"));
                        longitude = Double.parseDouble(jsonObject.getString("longitude"));
//                        Log.d("TAG", String.valueOf(latitude));
//                        Log.d("TAG", String.valueOf(longitude));


                        Message message = new Message();
                        message.what = GET_INFORMATION;
                        handler.sendMessage(message);

                        Thread.sleep(4000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //异步消息处理
    public android.os.Handler handler = new android.os.Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_INFORMATION:
                    speedShow.setText(speed);
                    powerShow.setText(power);
                    aMap.clear();
                    mapInit();
                    break;
                default:
                    break;
            }
        }

    };

    /**
     * 直接调用短信接口发短信
     *
     * @param phoneNumber
     * @param message
     */
    public void sendSMS(String phoneNumber, String message) {
        //获取短信管理器
        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
        //拆分短信内容（手机短信长度限制）
        List<String> divideContents = smsManager.divideMessage(message);
        for (String text : divideContents) {
            smsManager.sendTextMessage(phoneNumber, null, text, sentPI, deliverPI);
        }
    }

    public void mapInit() {
        if (aMap == null) {
            aMap = mapview.getMap();
        }
        //设置缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        //将地图移动到定位点
        aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(dealPosition(latitude) - 0.00259666, dealPosition(longitude) + 0.005337)));
        //添加图钉
        options.position(new LatLng(dealPosition(latitude) - 0.00259666, dealPosition(longitude) + 0.005337));
        aMap.addMarker(options);
    }
}
