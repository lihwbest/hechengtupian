/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.yoojia.zxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 * @author dswitkin@google.com (Daniel Switkin)
 * @author 陈小锅 (yoojia.chen@gmail.com)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private final Context mContext;

    private Camera mCamera;

    private Rect mFramingRect;

    private boolean mInitialized;
    private boolean mPreviewing;

    private int mRequestedFramingRectWidth;
    private int mRequestedFramingRectHeight;

    private final Point mScreenResolution = new Point();
    private final Point mCameraResolution = new Point();

    public CameraManager(Context context) {
        this.mContext = context;
    }

    /**
     * 开启相机设备
     * @throws IOException 当相机设备无法启动, 或者相机已经开启时, 抛出 IOException 异常.
     */
    public void open() throws IOException {
        if (! isOpen()) {
            mCamera = OpenCamera.open(OpenCamera.NO_REQUESTED_CAMERA);
            if (mCamera == null) {
                throw new IOException("Cannot open Camera device !");
            }
            initCamera();
        }else {
            throw new IOException("Camera was open !");
        }
    }

    /**
     * 关闭相机设备
     * @throws IOException 当相机设备尚未启动时, 抛出 IOException 异常.
     */
    public void close() throws IOException {
        if (isOpen()) {
            mCamera.release();
            mCamera = null;
            mFramingRect = null;
        }else {
            throw new IOException("Camera is not open !");
        }
    }

    /**
     * 设置相机预览回调接口
     * @param callback 预览回调接口
     */
    public void setPreviewCallback(Camera.PreviewCallback callback){
        mCamera.setOneShotPreviewCallback(callback);
    }

    /**
     * 指定相机预览SurfaceView
     * @param previewHolder SurfaceView Holder
     * @throws IOException
     */
    public void attachPreview(SurfaceHolder previewHolder) throws IOException {
        mCamera.setPreviewDisplay(previewHolder);
    }

    /**
     * 启动相机预览
     */
    public void startPreview() {
        if (mCamera != null && !mPreviewing) {
            mCamera.startPreview();
            mPreviewing = true;
        }
    }

    /**
     * 关闭相机预览
     */
    public void stopPreview() {
        if (mCamera != null && mPreviewing) {
            mCamera.stopPreview();
            mPreviewing = false;
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public boolean isOpen() {
        return mCamera != null;
    }

    //////////////

    public synchronized void setManualFramingRect(int width, int height) {
        if (mInitialized) {
            if (width > mScreenResolution.x) {
                width = mScreenResolution.x;
            }
            if (height > mScreenResolution.y) {
                height = mScreenResolution.y;
            }
            int leftOffset = (mScreenResolution.x - width) / 2;
            int topOffset = (mScreenResolution.y - height) / 2;
            mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + mFramingRect);
        } else {
            mRequestedFramingRectWidth = width;
            mRequestedFramingRectHeight = height;
        }
    }

    private void initCamera(){
        mCamera.setDisplayOrientation(90);
        if (!mInitialized) {
            mInitialized = true;
            initFromCameraParameters(mCamera);
            if (mRequestedFramingRectWidth > 0 && mRequestedFramingRectHeight > 0) {
                setManualFramingRect(mRequestedFramingRectWidth, mRequestedFramingRectHeight);
                mRequestedFramingRectWidth = 0;
                mRequestedFramingRectHeight = 0;
            }
        }
        Camera.Parameters parameters = mCamera.getParameters();
        try {
            setDesiredCameraParameters(mCamera, false);
        } catch (RuntimeException e) {
            // Reset:
            final String resets = parameters.flatten();
            Log.e(TAG, "- Camera rejected parameters. Setting only MINIMAL SAFE-MODE parameters");
            Log.e(TAG, "- Resetting to saved camera params: " + resets);
            parameters = mCamera.getParameters();
            parameters.unflatten(resets);
            try {
                mCamera.setParameters(parameters);
                setDesiredCameraParameters(mCamera, true);
            } catch (RuntimeException ee) {
                Log.e(TAG, "- Camera rejected even safe-mode parameters! NO CONFIGURATION");
            }
        }
    }

    private void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        display.getSize(mScreenResolution);
        Log.i(TAG, "- Screen resolution: " + mScreenResolution);
        Point resolved = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, mScreenResolution);
        mCameraResolution.set(resolved.x, resolved.y);
        Log.i(TAG, "- Camera resolution: " + mCameraResolution);
    }

    private void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setFocus( parameters,
                true, // auto focus
                true, // disable continuous
                safeMode);
        parameters.setPreviewSize(mCameraResolution.x, mCameraResolution.y);
        camera.setParameters(parameters);
        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (mCameraResolution.x != afterSize.width || mCameraResolution.y != afterSize.height)) {
            Log.w(TAG, "Camera said it supported preview size " + mCameraResolution.x + 'x' + mCameraResolution.y +
                    ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            mCameraResolution.x = afterSize.width;
            mCameraResolution.y = afterSize.height;
        }
    }

}
