package com.github.yoojia.zxing.camera;

import android.view.SurfaceHolder;

/**
 * 照相机预览接口，隐藏无用的接口
 * @author 陈小锅 (yoojia.chen@gmail.com)
 */
abstract class SurfaceViewReadyCallback implements SurfaceHolder.Callback {

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
