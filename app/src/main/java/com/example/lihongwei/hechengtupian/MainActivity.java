package com.example.lihongwei.hechengtupian;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.github.yoojia.zxing.qrcode.Encoder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

public class MainActivity extends AppCompatActivity {

    private ImageView mImg;
    private EditText editText;

    private ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImg = (ImageView) findViewById(R.id.img);
        editText = (EditText) findViewById(R.id.edittext);
//        mImg.post(new Runnable() {
//            @Override
//            public void run() {
//                final Bitmap bitmap = ((BitmapDrawable) mImg.getDrawable()).getBitmap();
//                new Thread() {
//                    @Override
//                    public void run() {
//                        super.run();
//                        Rect mRect = GetTransparentBounds(bitmap);
//                    }
//                }.start();
//            }
//        });
    }

    public void toCreate(View view) {
        final String text = editText.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {

            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("正在生成图片~");
            }

            mProgressDialog.show();

            new Thread() {
                @Override
                public void run() {
                    super.run();
                    final Bitmap shareBitmap = createShareBitmap(text);
                    saveImageToGallery(MainActivity.this, shareBitmap);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            mImg.setImageBitmap(shareBitmap);
                        }
                    });
                }
            }.start();
        }
    }

    /**
     * 计算图片里的透明区域
     *
     * @param bitmap
     * @return
     */
    public Rect getTransparentBounds(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int left = -1;// x min
        int top = -1;//  y min  (left,top)左上角坐标

        int right = -1;// x max
        int bottom = -1;//y max (right,bottom)右下角坐标

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bitmap.getPixel(x, y) == 0) {
                    if (left == -1) {
                        left = x;
                    } else if (left != -1) {
                        left = Math.min(x, left);
                    }

                    if (top == -1) {
                        top = y;
                    } else if (top != -1) {
                        top = Math.min(y, top);
                    }

                    if (right == -1) {
                        right = x;
                    } else if (right != -1) {
                        right = Math.max(x, right);
                    }

                    if (bottom == -1) {
                        bottom = y;
                    } else if (bottom != -1) {
                        bottom = Math.max(y, bottom);
                    }
                }
            }
        }

        Log.i("MainActivity", String.format("(%s,%s,%s,%s)", String.valueOf(left), String.valueOf(top), String.valueOf(right), String.valueOf(bottom)));

        return new Rect(left, top, right, bottom);
    }

    public Bitmap createShareBitmap(String text) {
        Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.myimg);

        final int dimension = 900;
        final Encoder encoder = new Encoder.Builder()
                .setBackgroundColor(Color.WHITE) // 指定背景颜色，默认为白色
                .setCodeColor(Color.BLACK) // 指定编码块颜色，默认为黑色
                .setOutputBitmapWidth(dimension) // 生成图片宽度
                .setOutputBitmapHeight(dimension) // 生成图片高度
                .setOutputBitmapPadding(1) // 设置为没有白边
                .build();

        final Bitmap _QRCodeImage = encoder.encode(text);

        //生成二维码
//        final Bitmap _QRCodeImage = createQRCode(text, 800);

        Bitmap newBitmap = createBitmap(srcBitmap, _QRCodeImage);

        srcBitmap.recycle();
        _QRCodeImage.recycle();

        return newBitmap;
    }


    /**
     * 图片合成
     * 将watermark画到src透明区域里
     *
     * @return
     */
    private Bitmap createBitmap(Bitmap src, Bitmap watermark) {
        if (src == null || watermark == null) {
            return null;
        }

        int w = src.getWidth();
        int h = src.getHeight();

        //获取src的透明区域
        Rect rect = getTransparentBounds(src);

        //将watermark大小调整为src透明区域的大小
        Bitmap newBitmap = zoomImg(watermark, rect.right - rect.left, rect.bottom - rect.top);
        watermark.recycle();
        watermark = newBitmap;

        //create the new blank bitmap
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);//创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        //draw src into
        cv.drawBitmap(src, 0, 0, null);//在 0，0坐标开始画入src
        cv.drawBitmap(watermark, rect.left, rect.top, null);
        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);//保存
        //store
        cv.restore();//存储
        return newb;
    }

    /**
     * 处理图片
     *
     * @param bm        所要转换的bitmap
     * @param newWidth  新的宽
     * @param newHeight 新的高
     * @return 指定宽高的bitmap
     */
    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片   www.2cto.com
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    public static void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "kouliang");
        String path = "";
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".png";
        File file = new File(appDir, fileName);
        path = file.getPath();

        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 0, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
    }

    public Bitmap createQRCode(String str, int widthAndHeight) {
        try {
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            BitMatrix matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * width + x] = Color.BLACK;
                    } else {
                        pixels[y * width + x] = Color.WHITE;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

}
