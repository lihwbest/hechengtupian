#主要功能
通过[ZXingMiNi](https://github.com/yoojia/ZXingMini)生成一张二维码图片，将二维码图图片合成到另一张含有透明区域的图片里。
#获取图片中的透明区域
```java  
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
```
#图片合成
```java  
  
private Bitmap createBitmap(Bitmap srcBitmap, Bitmap qrcodeBitmap) {
        if (srcBitmap == null || qrcodeBitmap == null) {
            return null;
        }

        int w = srcBitmap.getWidth();
        int h = srcBitmap.getHeight();

        //获取src的透明区域
        Rect rect = getTransparentBounds(srcBitmap);

        //将watermark大小调整为src透明区域的大小
        Bitmap newBitmap = zoomImg(qrcodeBitmap, rect.right - rect.left, rect.bottom - rect.top);
        qrcodeBitmap.recycle();
        qrcodeBitmap = newBitmap;

        //create the new blank bitmap
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);//创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        //draw src into
        cv.drawBitmap(srcBitmap, 0, 0, null);//在 0，0坐标开始画入src
        cv.drawBitmap(qrcodeBitmap, rect.left, rect.top, null);
        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);//保存
        //store
        cv.restore();//存储
        return newb;
    }
  
```

