package tv.danmaku.ijk.media.example.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.os.AsyncTask;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

//import java.io.IOException;
//
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;

//const String url = "http://172.16.60.179:5000/sensor";
//const String media_type = "application/json; charset=utf-8";

public class SensorService extends Service implements SensorEventListener {

    final String url = "http://172.16.60.179:5000/sensor";
    final String media_type = "application/json; charset=utf-8";

    private SensorManager sensorManager;

    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] rotationMatrix2 = new float[9];
    private float[] orientationAngles = new float[3];
    private float[] quaternion = new float[4];

//    private OkHttpClient okHttpClient;

    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service onStartCommand", Toast.LENGTH_LONG).show();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        assert sensorManager != null;
        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
        );

        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
        );

//        okHttpClient = new OkHttpClient();

        return START_STICKY;


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(this, "service onDestroy", Toast.LENGTH_LONG).show();

        sensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(
                        sensorEvent.values,
                        0,
                        accelerometerReading,
                        0,
                        accelerometerReading.length
                );
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(
                        sensorEvent.values,
                        0,
                        magnetometerReading,
                        0,
                        magnetometerReading.length
                );
                break;
            default:
                break;
        }

        SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
        );

        SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Z,
                SensorManager.AXIS_MINUS_X,
                rotationMatrix2
        );

        SensorManager.getOrientation(
                rotationMatrix2,
                orientationAngles
        );

        SensorManager.getQuaternionFromVector(
                quaternion,
                rotationMatrix2
        );

//        new UpdateTask().execute(Arrays.toString(orientationAngles));
        HttpConnectionUtil util = new HttpConnectionUtil();

        Map<String, String> map = new HashMap<>();

        map.put("orientationAngles", Arrays.toString(orientationAngles));
//        long s = System.nanoTime();
        util.postRequset(url, map);
//        long e = System.nanoTime();

//        Log.i("TIME", String.valueOf((e - s)/1000000));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private class UpdateTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            HttpConnectionUtil util = new HttpConnectionUtil();

            Map<String, String> map = new HashMap<>();

            map.put("orientationAngles", strings[0]);
//            long s = System.nanoTime();
            util.postRequset(url, map);
//            long e = System.nanoTime();

//            Log.i("TIME", String.valueOf((e - s)/1000000));
//
            return null;
        }
    }
//


    private class HttpConnectionUtil {
//        public  HttpConnectionUtil http = new HttpConnectionUtil();
//
//        public  HttpConnectionUtil getHttp() {
//            return http;
//        }

        public String getRequset(final String url) {
            final StringBuilder sb = new StringBuilder();
            FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    HttpURLConnection connection = null;
                    BufferedReader reader = null;
                    try {
                        URL requestUrl = new URL(url);
                        connection = (HttpURLConnection) requestUrl.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(8000);
                        connection.setReadTimeout(8000);
                        if (connection.getResponseCode() == 200) {
                            InputStream in = connection.getInputStream();
                            reader = new BufferedReader(new InputStreamReader(in));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            System.out.println(sb);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                        if (connection != null) {
                            connection.disconnect();//断开连接，释放资源
                        }
                    }
                    return sb.toString();
                }
            });
            new Thread(task).start();
            String s = null;
            try {
                s = task.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return s;
        }

        public String postRequset(final String url, final Map<String, String> map) {
            final StringBuilder sb = new StringBuilder();
            FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    HttpURLConnection connection = null;
                    BufferedReader reader = null;
                    try {
                        URL requestUrl = new URL(url);
                        connection = (HttpURLConnection) requestUrl.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setConnectTimeout(8000);//链接超时
                        connection.setReadTimeout(8000);//读取超时
                        //发送post请求必须设置
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setUseCaches(false);
                        connection.setInstanceFollowRedirects(true);
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        DataOutputStream out = new DataOutputStream(connection
                                .getOutputStream());
                        StringBuilder request = new StringBuilder();
                        for (String key : map.keySet()) {
                            request.append(key + "=" + URLEncoder.encode(map.get(key), "UTF-8") + "&");
                        }
                        out.writeBytes(request.toString());//写入请求参数
                        out.flush();
                        out.close();
                        if (connection.getResponseCode() == 200) {
                            InputStream in = connection.getInputStream();
                            reader = new BufferedReader(new InputStreamReader(in));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            System.out.println(sb);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (reader != null) {
                            reader.close();//关闭流
                        }
                        if (connection != null) {
                            connection.disconnect();//断开连接，释放资源
                        }
                    }
                    return sb.toString();
                }
            });
            new Thread(task).start();
            String s = null;
            try {
                s = task.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return s;
        }

    }
}
