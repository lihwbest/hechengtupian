package com.github.yoojia.zxing.camera;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 * @since 1.0
 */
public class Cameras {

    private final static String TAG = Cameras.class.getSimpleName();

    private final SurfaceView mPreviewSurfaceView;
    private final CameraManager mCameraManager;
    private boolean mIsSurfaceViewReady = false;

    private FocusManager mFocusManager;

    private final SurfaceViewReadyCallback mViewReadyCallback = new SurfaceViewReadyCallback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mIsSurfaceViewReady = true;
            Log.d(TAG, "- Preview SurfaceView NOW ready, open camera by CameraManager");
            mPreviewTask.run();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            super.surfaceDestroyed(holder);
            mIsSurfaceViewReady = false;
            Log.d(TAG, "surfaceDestroy");
        }
    };

    private OneshotTask mPreviewTask = new OneshotTask() {
        @Override
        public void doThis() {
            Log.d(TAG, "- NOW open camera and start preview...");
            try {
                mCameraManager.attachPreview(mPreviewSurfaceView.getHolder());
                mCameraManager.startPreview();
                if (mFocusManager.isAutoFocusEnabled()) {
                    mFocusManager.startAutoFocus(mCameraManager.getCamera());
                }
            } catch (IOException e) {
                Log.e(TAG, "- Cannot attach to preview", e);
            }
        }
    };

    public Cameras(SurfaceView previewSurfaceView) {
        mPreviewSurfaceView = previewSurfaceView;
        mCameraManager = new CameraManager(previewSurfaceView.getContext());
        final SurfaceHolder holder = mPreviewSurfaceView.getHolder();
        holder.addCallback(mViewReadyCallback);
        mFocusManager = new FocusManager();
    }

    public void start() {
        Log.d(TAG, "- Try open camera and start preview...");
        try {
            if (!mCameraManager.isOpen()) {
                mCameraManager.open();
            }
            mPreviewTask.ready();
            if (mIsSurfaceViewReady) {
                Log.d(TAG, "openCameraDirectly");
                mPreviewTask.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "- Cannot open camera", e);
        }
    }


    public void stop() {
        Log.d(TAG, "- Try stop preview and close camera...");
        Log.d(TAG, "stopCameraDirectly");
        mCameraManager.getCamera().setPreviewCallback(null);
        mFocusManager.stopAutoFocus(mCameraManager.getCamera());
        mCameraManager.stopPreview();
        try {
            mCameraManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startAutoFocus(int period, Camera.AutoFocusCallback callback) {
        mFocusManager.setAutoFocus(period, callback);
        if (mCameraManager.isOpen() && mFocusManager.isAutoFocusEnabled()) {
            mFocusManager.startAutoFocus(mCameraManager.getCamera());
        }
    }

    static abstract class OneshotTask implements Runnable {
        private AtomicBoolean ready = new AtomicBoolean();

        public void ready() {
            ready.set(true);
        }

        @Override
        public final void run() {
            if (ready.get()) {
                ready.set(false);
                doThis();
            }
        }

        protected abstract void doThis();
    }
}
