package com.cryptix.mshack;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "CAM";

    SurfaceView svCamera;
    SurfaceHolder surfaceHolder;
    Camera camera;
    Button btnTakePhoto;
    Camera.PictureCallback picCallBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        svCamera = (SurfaceView) findViewById(R.id.surfaceViewCamera);
        btnTakePhoto = (Button) findViewById(R.id.btnTakePhoto);


        int camPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (camPerm != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    231);
        } else {
            checkCameraSizes();
        }

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int storePerm = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (storePerm != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            222);
                } else {
                    Timer timerObj = new Timer();
                    TimerTask timerTaskObj = new TimerTask() {
                        public void run() {
                            //perform your action here
                            capturePhoto();
                        }
                    };
                    timerObj.schedule(timerTaskObj, 0, 5000);

                }
            }
        });

        picCallBack = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                String fileName = "photo"+".jpg";
                File myPhoto = new File(Environment.getExternalStorageDirectory(), fileName);
                Log.d("TAG", Base64.encodeToString(data,Base64.DEFAULT));
                try {
                    FileOutputStream fos = new FileOutputStream(myPhoto);
                    fos.write(data);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } FileInputStream fis = null;

                try {
                    fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(),"photo.jpg"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Bitmap bitmapA = BitmapFactory.decodeStream(fis);
                Matrix matrix = new Matrix();

                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapA , 0, 0, bitmapA .getWidth(), bitmapA .getHeight(), matrix, true);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream .toByteArray();




                final String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

                String API_BASE_URL = "http://168.61.48.201/mshack/mshack/index.php/welcome/getimage";

                Volley.newRequestQueue(MainActivity.this).add(new StringRequest(Request.Method.POST, API_BASE_URL, new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                     Log.d("tagresponse", response);
                    }
                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        HashMap<String, String> data = new HashMap<>();
                        data.put("img", encoded); // edit params
                        return data;
                    }
                });

                try {
                    camera.startPreview();
                } catch (Exception e) {
                    Log.d(TAG, "onPictureTaken: Could not restart preview", e);
                }
            }
        };

    }

    void capturePhoto() {
        camera.takePicture(null, null, picCallBack);


    }

    void checkCameraSizes () {
        camera = Camera.open();

        final Camera.Parameters camParams = camera.getParameters();
        for (Camera.Size picSize : camParams.getSupportedPictureSizes()) {
            Log.d(TAG, "picSize: " + picSize.width + " " + picSize.height);
        }
        for (Camera.Size prevSize : camParams.getSupportedPreviewSizes()) {
            Log.d(TAG, "prevSize: " + prevSize.width + " " + prevSize.height);
        }
        for (Camera.Size vidSize : camParams.getSupportedVideoSizes()) {
            Log.d(TAG, "vidSize: " + vidSize.width + " " + vidSize.height);
        }

        camParams.setPictureSize(640,480);
        surfaceHolder = svCamera.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.setDisplayOrientation(90);
                    camera.setParameters(camParams);
                    camera.startPreview();
                } catch (IOException e) {
                    Log.d(TAG, "surfaceCreated: Could not start preview" );
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                WindowManager winMan = (WindowManager) getSystemService(WINDOW_SERVICE);
                Display display = winMan.getDefaultDisplay();

                if (holder.getSurface() == null) {
                    return;
                }

                try {
                    camera.stopPreview();
                } catch (Exception e) {
                    Log.e(TAG, "surfaceChanged: ", e);
                }

                Camera.Parameters changedParams = camera.getParameters();

                if (display.getRotation() == Surface.ROTATION_0) {
                    camera.setDisplayOrientation(90);

                }
                if (display.getRotation() == Surface.ROTATION_90) {
                    camera.setDisplayOrientation(0);

                }
                if (display.getRotation() == Surface.ROTATION_180) {
                    camera.setDisplayOrientation(270);


                }
                if (display.getRotation() == Surface.ROTATION_270) {
                    camera.setDisplayOrientation(180);
                }

                try {
                    camera.setParameters(changedParams);
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "surfaceChanged: ", e);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }

    @Override
    protected void onPause() {
        if (camera != null) {
            camera.stopPreview();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera != null) {
            try {
                camera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 231) {
            if (permissions[0].equals(Manifest.permission.CAMERA)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkCameraSizes();
                }
            } else {
                Toast.makeText(this, "You did not give permission", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == 222) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    capturePhoto();
                }
            } else {
                Toast.makeText(this, "You did not give permission", Toast.LENGTH_SHORT).show();
            }
        }
    }
}