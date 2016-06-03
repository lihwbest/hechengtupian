package com.github.yoojia.zxing.qrcode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;

/**
 * @author :   Yoojia.Chen (yoojia.chen@gmail.com)
 * @since 1.0
 * 二维码解码线程
 */
public abstract class DecodeTask extends AsyncTask<CameraPreview, Bitmap, String> {

    private final Decoder mQRCodeDecode;

    public DecodeTask(Decoder qrCodeDecode) {
        mQRCodeDecode = qrCodeDecode;
    }

    @Override
    final protected String doInBackground(CameraPreview... params) {
        if (params.length == 0){
            throw new IllegalArgumentException("Parameter required when call 'execute(CameraPreview)'; ");
        }
        final Bitmap progress = params[0].capture();
        this.publishProgress(progress);
        return mQRCodeDecode.decode(progress);
    }

    @Override
    final protected void onPostExecute(String s) {
        if (!TextUtils.isEmpty(s)){
            onPostDecoded(s);
        }
    }

    /**
     * 解码完成
     * @param result 解码完成
     */
    protected abstract void onPostDecoded(String result);

    @Override
    final protected void onProgressUpdate(Bitmap... values) {
        onDecodeProgress(values[0]);
    }

    /**
     * 解码的图片
     * @param capture 图片
     */
    protected void onDecodeProgress(Bitmap capture){
        // Override if need
    }

}
