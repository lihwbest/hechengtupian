package com.github.yoojia.zxing.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 陈小锅 (yoojia.chen@gmail.com)
 */
public class FocusManager {

    private final AtomicInteger mPeriod = new AtomicInteger(0);

    private final Handler mFocusHandler = new Handler(Looper.getMainLooper());

    private AutoFocusTask mAutoFocusTask;

    private Camera.AutoFocusCallback mAutoFocusCallback;
    private boolean mEnabledAutoFocus;

    /**
     * 请求相机执行对焦动作
     *
     * @param camera 相机对象
     */
    public void requestAutoFocus(Camera camera, Camera.AutoFocusCallback cb) {
        camera.autoFocus(cb);
    }

    /**
     * 开启定时自动对焦
     *
     * @param camera 相机对象
     */
    public void startAutoFocus(Camera camera) {
        final String mode = camera.getParameters().getFocusMode();
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode) || Camera.Parameters.FOCUS_MODE_MACRO.equals(mode)) {
            // Remove pre task
            if (mAutoFocusTask != null) {
                mFocusHandler.removeCallbacks(mAutoFocusTask);
            }
            mAutoFocusTask = new AutoFocusTask(camera, mAutoFocusCallback);
            mFocusHandler.post(mAutoFocusTask);
        }
    }

    /**
     * @param ms 定时,单位:毫秒
     * @param cb
     */
    public void setAutoFocus(int ms, Camera.AutoFocusCallback cb) {
        Log.d("FocusManager", "setAutoFocus:" + ms + ", " + cb);
        if (ms < 100) {
            throw new IllegalArgumentException("Auto Focus period time must more than 100ms !");
        }
        mPeriod.set(ms);
        mAutoFocusCallback = cb;
        mEnabledAutoFocus = cb != null;
    }

    /**
     * 停止自动对焦
     */
    public void stopAutoFocus(Camera camera) {
        mEnabledAutoFocus = false;
        mFocusHandler.removeCallbacks(mAutoFocusTask);
        camera.cancelAutoFocus();
    }

    public boolean isAutoFocusEnabled() {
        return mEnabledAutoFocus;
    }

    private class AutoFocusTask implements Runnable {

        private final Camera mCamera;
        private final Camera.AutoFocusCallback mAutoFocusCallback;

        private AutoFocusTask(Camera camera, Camera.AutoFocusCallback cb) {
            mCamera = camera;
            mAutoFocusCallback = cb;
        }

        @Override
        public void run() {
            requestAutoFocus(mCamera, mAutoFocusCallback);
            final int period = mPeriod.get();
            if (period > 0) {
                mFocusHandler.postDelayed(mAutoFocusTask, period);
            }
        }
    }
}
