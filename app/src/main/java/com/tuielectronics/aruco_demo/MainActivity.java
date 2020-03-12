package com.tuielectronics.aruco_demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "[main]";
    private volatile boolean permissionGranted = false;

    private FrameLayout cameraFrameLayout;
    private Camera camera;
    private CameraProcess cameraProcess;
    private SurfaceView mSurfaceView;
    private ImageView imageViewCameraPreview;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:

                    Log.d(TAG, "navigation_home pressed");
                    toggleCamera(false);

                    return true;
                case R.id.navigation_dashboard:

                    Log.d(TAG, "navigation_dashboard pressed");
                    toggleCamera(true);

                    return true;
            }

            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        /*
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= 23.0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "Permission request");
                requestAppPermission();
                permissionGranted = false;

            } else {
                permissionGranted = true;
            }
        } else {
            permissionGranted = true;
        }

        cameraFrameLayout = findViewById(R.id.content_camera);
        mSurfaceView = findViewById(R.id.surface_view);
        cameraProcess = new CameraProcess(this, mSurfaceView);
        cameraFrameLayout.addView(cameraProcess);
        //toggleCamera(true);

        imageViewCameraPreview = findViewById(R.id.image_view_camera_preview);
        imageViewCameraPreview.setZ(10f);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUIEvent(UIEvent event) {
        if (event.type.equals("camera")) {
            if (event.topic.equals("refresh")) {
                Bitmap bm = Bitmap.createScaledBitmap(cameraProcess.bitmapOpenCVPreview, imageViewCameraPreview.getWidth(), imageViewCameraPreview.getHeight(), false);
                imageViewCameraPreview.setImageBitmap(bm);
                //Log.d(TAG, "camera refreshed");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        createQuitDialog();
    }

    private void createQuitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int arg1) {
                        finish();
                        //MainActivity.super.onBackPressed();
                        System.exit(0);
                    }
                }).create().show();
    }

    private void requestAppPermission() {
        final String[] PERMISSIONS_STRING = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS_STRING, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult:" + String.valueOf(requestCode));
        if (requestCode == 1) {
            boolean appPermissionCheckTest = true;
            for (int i = 0; i < grantResults.length; i++) {
                Log.d(TAG, "onRequestPermissionsResult: grantResults: " + String.valueOf(grantResults[i]));

                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Failed")
                            .setMessage("Camera permission required to run this App")

                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    finish();
                                    System.exit(0);
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    finish();
                                    System.exit(0);
                                }
                            })
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int arg1) {
                                    finish();
                                    System.exit(0);
                                }
                            }).create().show();
                    appPermissionCheckTest = false;
                    break;
                }
            }
            if (appPermissionCheckTest) permissionGranted = true;
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        if (permissionGranted) {
            Log.d(TAG, "onRequestPermissions finished");
        }
    }

    private void toggleCamera(boolean open) {
        if (!open) {
            if (camera != null) {
                cameraProcess.setCamera(null);
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            }
        } else {
            if (camera == null) {
                camera = getCameraInstance();

                cameraProcess.setCamera(camera);
            }
        }
    }

    private static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
            Log.d(TAG, "camera number:  " + Camera.getNumberOfCameras());
        } catch (Exception e) {
            Log.d(TAG, "Could not open camera");
        }
        return c;
    }
}
