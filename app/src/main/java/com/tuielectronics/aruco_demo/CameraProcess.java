package com.tuielectronics.aruco_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class CameraProcess extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "[camera preview]";
    private Camera mCamera;
    private SurfaceView mSurfaceView;

    private int cameraFrameWidth;
    private int cameraFrameHeight;
    //set the image preview size, reduce this size will increase the FPS
    private final int bitmapPreviewWidth = 480;
    private final int bitmapPreviewHeight = 320;

    public static Bitmap bitmapOpenCVPreview;
    private volatile static Bitmap bitmapOpenCVRaw;
    // pixel buffer to store the result of OpenCV image
    private volatile static int[] pixelsOpenCV;

    private volatile boolean bProcessing = false; // thread busy flag, to prevent the over-flow
    private volatile long timeImageRefreshed = 0;

    public CameraProcess(Context context, SurfaceView s) {
        super(context);
        mSurfaceView = s;
        mSurfaceView.getHolder().setFormat(ImageFormat.NV21);
        mSurfaceView.getHolder().addCallback(this);
    }

    private void startPreview(SurfaceHolder holder) {
        if (mCamera == null) {
            return;
        }
        try {
            configureCamera();
            //mCamera.setPreviewCallback(this);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.addCallbackBuffer(new byte[((cameraFrameWidth * cameraFrameHeight) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            Log.d(TAG, "startPreview Callback buffer size = " + ((cameraFrameWidth * cameraFrameHeight) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8 + ", getBitsPerPixel = " + ImageFormat.getBitsPerPixel(ImageFormat.NV21));
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }


    private void configureCamera() {
        Camera.Parameters p = mCamera.getParameters();
        List<Camera.Size> sizes = p.getSupportedPreviewSizes();
        boolean checked = false;
        // use 720P by default
        for (Camera.Size size : sizes) {
            Log.d(TAG, "size: w:" + String.valueOf(size.width) + " ,h: " + String.valueOf(size.height));
            if (size.height >= 720 && size.height <= 768) {
                cameraFrameWidth = size.width;
                cameraFrameHeight = size.height;
                checked = true;
            }
        }
        // for some low spec cameras
        if (!checked) {
            for (Camera.Size size : sizes) {

                if (size.height >= 480) {
                    cameraFrameWidth = size.width;
                    cameraFrameHeight = size.height;
                    break;
                }
            }
        }
        Log.d(TAG, "size preset: w:" + String.valueOf(cameraFrameWidth) + " ,h: " + String.valueOf(cameraFrameHeight));

        pixelsOpenCV = new int[bitmapPreviewWidth * bitmapPreviewHeight];
        bitmapOpenCVRaw = Bitmap.createBitmap(bitmapPreviewWidth, bitmapPreviewHeight, Bitmap.Config.ARGB_8888);
        // preview bitmap is 90 rotated
        bitmapOpenCVPreview = Bitmap.createBitmap(bitmapPreviewHeight, bitmapPreviewWidth, Bitmap.Config.ARGB_8888);

        p.setPreviewSize(cameraFrameWidth, cameraFrameHeight);
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        //p.setPreviewFrameRate(10);

        mCamera.setDisplayOrientation(90);
        p.setRecordingHint(true);//speed up?

        //p.setPreviewFpsRange(15000, 15000);
        p.setPreviewFormat(ImageFormat.NV21);
        //p.setAntibanding(ANTIBANDING_AUTO);

        mCamera.setParameters(p);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged");
    }

    public void setCamera(Camera c) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mCamera = c;
        if (mCamera != null) {
            startPreview(getHolder());
        }
    }

    public void onPreviewFrame(final byte[] data, Camera camera) {
        // re-enqueue this buffer
        mCamera.addCallbackBuffer(data);
        if (camera != mCamera) {
            Log.d(TAG, "Unknown Camera!");
            return;
        }
        if (mSurfaceView.getHolder().getSurface().isValid()) {
            //Log.d(TAG, "createQRScanRunnableNew0");
            if (!bProcessing) {
                bProcessing = true;
                new Thread() {
                    @Override
                    public void run() {
                        runProcess(data, cameraFrameWidth, cameraFrameHeight, pixelsOpenCV, bitmapPreviewWidth, bitmapPreviewHeight);
                    }
                }.start();
                if (System.currentTimeMillis() - timeImageRefreshed >= 100) {
                    timeImageRefreshed = System.currentTimeMillis();

                    bitmapOpenCVRaw.eraseColor(Color.WHITE);
                    bitmapOpenCVRaw.setPixels(pixelsOpenCV, 0, bitmapPreviewWidth, 0, 0, bitmapPreviewWidth, bitmapPreviewHeight);

                    Matrix matrix = new Matrix();
                    matrix.preRotate(90);
                    bitmapOpenCVPreview = Bitmap.createBitmap(bitmapOpenCVRaw, 0, 0, bitmapOpenCVRaw.getWidth(), bitmapOpenCVRaw.getHeight(), matrix, true);
                    EventBus.getDefault().post(new UIEvent("camera", "refresh", null, null));
                }
            }
        } else {
            Log.d(TAG, "Invalid Surface!");
        }
    }

    // load jni library
    static {
        System.loadLibrary("imageProcess");
    }

    private native double[] openCVProcess(int width, int height,
                                          byte[] NV21FrameData, int[] px, int px_width, int px_height);

    // process the post-OpenCV data here
    public void runProcess(final byte[] imageFrameData, final int frame_width, final int frame_height, int[] pixels, final int pixel_width, final int pixel_height) {

        double[] arucoPositionArray = openCVProcess(frame_width, frame_height, imageFrameData, pixels, pixel_width, pixel_height);
        if (arucoPositionArray.length / 4 > 0) {
            Log.d(TAG, "aruco detected: " + arucoPositionArray.length / 4);
        }
        bProcessing = false;
    }
}
