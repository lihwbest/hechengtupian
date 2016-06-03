package com.github.yoojia.zxing.qrcode;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.github.yoojia.zxing.camera.Cameras;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @since 1.0
 * 封装扫描支持功能
 */
public class QRCodeSupport {

    public static final String TAG = QRCodeSupport.class.getSimpleName();

    private final Decoder mQRCodeDecode = new Decoder.Builder().build();
    private ImageView mCapturePreview = null;
    private OnResultListener mOnResultListener;
    private final Cameras mCameras;
    private Camera.PreviewCallback mPreviewFrameCallback;

    public QRCodeSupport(SurfaceView surfaceView) {
        this(surfaceView, null);
    }

    public QRCodeSupport(SurfaceView surfaceView, OnResultListener listener) {
        mPreviewFrameCallback = new Camera.PreviewCallback() {
            private PreviewQRCodeDecodeTask mDecodeTask;

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mDecodeTask != null) {
                    mDecodeTask.cancel(true);
                }
                Log.d(TAG, "onPreviewFrame");
                mDecodeTask = new PreviewQRCodeDecodeTask(mQRCodeDecode);
                CameraPreview preview = new CameraPreview(data, camera);
                mDecodeTask.execute(preview);
            }
        };
        mCameras = new Cameras(surfaceView);

        mOnResultListener = listener;
    }

    public void onResume() {
        mCameras.start();
    }

    public void onPause() {
        mCameras.stop();
    }

    public void startAuto(int period) {
        mCameras.startAutoFocus(period, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    camera.setOneShotPreviewCallback(mPreviewFrameCallback);
                }
            }
        });
    }

    public void setOnResultListener(OnResultListener onResultListener) {
        mOnResultListener = onResultListener;
    }

    public void setCapturePreview(ImageView capturePreview) {
        mCapturePreview = capturePreview;
    }

    private class PreviewQRCodeDecodeTask extends DecodeTask {

        public PreviewQRCodeDecodeTask(Decoder qrCodeDecode) {
            super(qrCodeDecode);
        }

        @Override
        protected void onPostDecoded(String result) {
            if (mOnResultListener == null) {
                Log.w(TAG, "WARNING ! QRCode result ignored !");
            } else {
                mOnResultListener.onScanResult(result);
            }
        }

        @Override
        protected void onDecodeProgress(Bitmap capture) {
            Log.d(TAG, "onDecodeProgress");
            if (mCapturePreview != null) {
                mCapturePreview.setImageBitmap(capture);
            }
        }
    }

    public interface OnResultListener {
        void onScanResult(String notNullResult);
    }
}
