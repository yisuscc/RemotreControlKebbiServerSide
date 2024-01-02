package com.nuwarobotics.sample.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.camera.common.Constants;
import com.nuwarobotics.service.camera.sdk.CameraSDK;
import com.nuwarobotics.service.camera.sdk.OutputData;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import ContainerSocket.DataType;
import ContainerSocket.SocketByteContainer;


public class FrameStreamingActivity extends AppCompatActivity {
    private CameraSDK mCameraSDK;

    private ImageView mImageFrame;
    private TextView vTextIP;
    private TextView vTextPort;
    private NuwaRobotAPI mRobot;
    private final Handler mHandler = new Handler();
    final int WIDTH = 1280;
    final int HEIGHT = 768;
    Integer portNumber = 4169;
    String ip = "LoremIpsum";
    private ServerSocket server;
    private  ServerSocket server2;
    private Socket client;
    private Socket client2;
    private AtomicBoolean sendAllowed  = new AtomicBoolean(true); // this one is for concurrency
    private AtomicBoolean sendJSONINfoAllowed = new AtomicBoolean(true); // this one to reduce the frecuency for sending the jsons
    private AtomicBoolean streamingFlag = new AtomicBoolean(true);
    private final CameraSDK.CameraSDKCallback mCameraSDKCallback = new CameraSDK.CameraSDKCallback() {
        @Override
        public void onConnected(boolean b) {
            Log.d("jesus", "" + b);
        }

        @Override
        public void onOutput(Map<Integer, OutputData> map) {

          if(sendAllowed.get()&& streamingFlag.get() && sendJSONINfoAllowed.get())  {
                for (Integer key : map.keySet()) {
                    OutputData ouputData = map.get(key);
                    Log.i("jesus", "" + ouputData.data);

                        sendSocketContainer(DataType.JSON, ouputData.data.getBytes());


                }
            }
        }

        @Override
        public void onPictureTaken(String s) {
//can ignnore
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String your_app_package_name = getPackageName();
        IClientId id = new IClientId(your_app_package_name);
        mRobot = new NuwaRobotAPI(this, id);
        mRobot.registerRobotEventListener(robotEventListener);
        mCameraSDK = new CameraSDK(this);
        mCameraSDK.register(mCameraSDKCallback, Constants.FACE_RECOGNITION, FrameStreamingActivity.class.getName());
        setContentView(R.layout.activity_sample);

        mImageFrame = findViewById(R.id.img_frame);
        vTextIP = findViewById(R.id.ipView);
        vTextPort = findViewById(R.id.portView);
        new Thread(() -> {
            serverSocketCreation();
            receiveCommand();
        }).start();

        startStreaming();
    }

    private final RobotEventListener robotEventListener = new RobotEventListener() {
        @Override
        public void onWikiServiceStart() {
            mRobot.requestSensor(NuwaRobotAPI.SENSOR_TOUCH);
        }

        @Override
        public void onWikiServiceStop() {

        }

        @Override
        public void onWikiServiceCrash() {

        }

        @Override
        public void onWikiServiceRecovery() {

        }

        @Override
        public void onWikiServiceError(int i) {

        }

        @Override
        public void onStartOfMotionPlay(String s) {

        }

        @Override
        public void onPauseOfMotionPlay(String s) {

        }

        @Override
        public void onStopOfMotionPlay(String s) {

        }

        @Override
        public void onCompleteOfMotionPlay(String s) {

        }

        @Override
        public void onPlayBackOfMotionPlay(String s) {

        }

        @Override
        public void onErrorOfMotionPlay(int i) {

        }

        @Override
        public void onPrepareMotion(boolean b, String s, float v) {

        }

        @Override
        public void onCameraOfMotionPlay(String s) {

        }

        @Override
        public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {

        }

        @Override
        public void onTouchEvent(int position, int i1) {
            Log.i("jesus", "i=" + position + "i1=" + i1);
            if (position == 4) {
                mRobot.showFace();
            } else if (position == 3) {
                mRobot.hideFace();
            }
        }

        @Override
        public void onPIREvent(int i) {

        }

        @Override
        public void onTap(int i) {

        }

        @Override
        public void onLongPress(int i) {

        }

        @Override
        public void onWindowSurfaceReady() {

        }

        @Override
        public void onWindowSurfaceDestroy() {

        }

        @Override
        public void onTouchEyes(int i, int i1) {

        }

        @Override
        public void onRawTouch(int i, int i1, int i2) {

        }

        @Override
        public void onFaceSpeaker(float v) {

        }

        @Override
        public void onActionEvent(int i, int i1) {

        }

        @Override
        public void onDropSensorEvent(int i) {

        }

        @Override
        public void onMotorErrorEvent(int i, int i1) {

        }
    };

    private void serverSocketCreation() {
        try {
            //server = new ServerSocket(portNumber);
            if (server != null && !server.isClosed()) {
                server.close();
            }
            server = new ServerSocket(0);
            ip = getLocalIP(this);
            portNumber = server.getLocalPort();
            //  Log.d("ServerSocketCreation", "seted the names" + "ip: " + ip + "port:" + portNumber);
            client = server.accept();
            mRobot.showFace();
            Log.i("jesus ", "Client connected");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onDestroy() {
        mCameraSDK.stopCameraStreaming();
        mRobot.release();

        mCameraSDK.release();
        try {
            if (client != null && client.isConnected())
                client.close();
            if (server != null && !server.isClosed())
                server.close();
        } catch (IOException e) {

        }
        super.onDestroy();
    }

    private void startStreaming() { // thread instead of void
        mCameraSDK
                .requestCameraStreaming(
                        WIDTH,
                        HEIGHT,
                        (code, bitmap) -> {
                            switch (code) {
                                case CameraSDK.CODE_NORMAL:
                                case CameraSDK.CODE_NORMAL_RESIZE:

                                  runOnUiThread(()-> {
                                      vTextIP.setText(ip);
                                      vTextPort.setText(portNumber.toString());
                                  });
                                    if (null != client && client.isConnected() && streamingFlag.get()) {
                                       if (!streamingFlag.get())
                                            break;
                                        runOnUiThread(() -> mImageFrame.setImageBitmap(bitmap));
                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                                           if(sendAllowed.get()){
                                               sendSocketContainer(DataType.BITMAP,stream.toByteArray());
                                           }
                                        runOnUiThread(() -> mImageFrame.setImageBitmap(bitmap));


                                    }

                                    break;
                                case CameraSDK.CODE_TOO_MANY_CLIENTS:
                                    // over 3 clients using currently.
                                case CameraSDK.CODE_ILLEGAL_RESOLUTION:
                                    // assigned resolution is illegal for now.
                            }
                        });

    }


    public String getLocalIP(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo info = wifiManager.getConnectionInfo();
        int ipAddress = info.getIpAddress();
        return String.format(Locale.ENGLISH, "%1$d.%2$d.%3$d.%4$d"
                , ipAddress & 0xff
                , ipAddress >> 8 & 0xff
                , ipAddress >> 16 & 0xff
                , ipAddress >> 24 & 0xff);
    }

    private final Runnable stopMovingRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != mRobot) {
                mRobot.move(0);
                mRobot.turn(0);
            }
        }
    };

    private void receiveCommand() {
        while (!server.isClosed() && client.isConnected() && !client.isClosed()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                 SocketByteContainer sbc = (SocketByteContainer)ois.readObject();
                if( sbc != null && sbc.getDataType() == DataType.JSON){
                    String str = new String(sbc.getDataArray());
                    JSONObject json = new JSONObject(str);
                    interpretCommand(json);
                }
            } catch (IOException | JSONException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private void interpretCommand(JSONObject command) {
        Log.i("Jesus", "interpretCommand " + command.toString());
        if (command != null) {
            String propiedad = null;
            String accion = null;
            try {
                propiedad = command.getString("property");
                accion = command.getString("action");


            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (propiedad != null && accion != null) {

                switch (propiedad) {
                    case "general":
                        switch (accion) {
                            case "disconnect":
                                streamingFlag.set(false);
                                if (client.isConnected()) {
                                    try {

                                        client.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                // we restart the server socket

                                new Thread(() -> {
                                    serverSocketCreation();
                                    receiveCommand();
                                }).start();

                                break;

                        }
                        break;
                    case "streaming":
                        switch (accion) {
                            case "start":
                                if (!streamingFlag.get()) {
                                    // I think i should use an atominc boolean as a flag to interrupt inside the thread
                                    streamingFlag.set(true);
                                    startStreaming();
                                }
                                break;
                            case "stop":
                                streamingFlag.set(false);
                                break;
                        }
                        break;
                    case "moving":
                        mHandler.removeCallbacks(stopMovingRunnable);
                        switch (accion) {
                            case "backward":
                                mRobot.move(-0.3f);
                                break;
                            case "frontward":
                                mRobot.move(0.3f);
                                break;
                            case "turnLeft":
                                mRobot.turn(90.0f);
                                break;
                            case "turnRight":
                                mRobot.turn(-90.0f);
                                break;
                        }
                        mHandler.postDelayed(stopMovingRunnable, 400);
                        break;
                }
            }
        }


    }



    private void sendSocketContainer(DataType dt, byte[] dataArray){
// complejidad  = k
        new Thread(()-> {
                Log.d("time","start");
            try {
               if(client!= null && !client.isClosed() && client.isConnected()){
                   sendAllowed.set(false);
                   if(dt.equals(DataType.JSON)){
                       sendJSONINfoAllowed.set(false);
                   }
                   ObjectOutputStream oos = new ObjectOutputStream((client.getOutputStream()));
                   SocketByteContainer sbc = new SocketByteContainer(dt,dataArray);
                   Log.d("sendSocket","sent:" + dt.toString());
                   oos.writeObject(sbc);
                   oos.flush();
                   sendAllowed.set(true);
                   if(dt.equals(DataType.JSON)) {
                       mHandler.postDelayed(() -> sendJSONINfoAllowed.set(true), 500);
                   }

               }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("time","finish");
        }).start();

    }

/*    private void sendBytes(TypeOfData td, byte[] DataBites) throws IOException {
            *//*another possible option is to  make a class
            that implements serializable ?
            said class wold have a flag that indicate isf it is json
            or a bitmap
            and gives you the  bitmap/jason
             *//*
        // first we determine what kind of data we are goint to send
       // in the header the two first bits are the start of sequence 11
        // 11_110101 if its a string 245
        // 11_110111 if its a bitmap 247
        byte header = 0;
        byte endSequence = (byte) 255;
        //TODO;: Change to switch
        if(td.equals(TypeOfData.BITMAP)){
            header = (byte) 247;
        }else if(td.equals(TypeOfData.JSON)){
            header = (byte) 245;
        }
       byte[] headerArray= new byte[1];
        headerArray[0] = header;
        byte[] arrayToSend = concat(headerArray,DataBites);
        if(client != null && client.isConnected() && streamingFlag.get()){
            // TODO: the sending mechanism
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            dos.writeInt(arrayToSend.length);
            dos.write(arrayToSend);

        }




    }*/
    private static byte[] concat(byte[]... arrays) {
        //from https://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }



}