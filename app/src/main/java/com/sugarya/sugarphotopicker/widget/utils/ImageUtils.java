package com.sugarya.sugarphotopicker.widget.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by Ethan on 2017/11/22.
 * 图片相关操作的工具类
 */

public class ImageUtils {

    /**
     * @param uri
     * @param targetWidth  限制宽度
     * @param targetHeight 限制高度
     * @return
     * @throws Exception
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri, float targetWidth, float targetHeight){
        Bitmap bitmap = null;
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            if (input != null) {
                input.close();
            }
            //获取原始图片大小
            int originalWidth = onlyBoundsOptions.outWidth;
            int originalHeight = onlyBoundsOptions.outHeight;
            if ((originalWidth == -1) || (originalHeight == -1))
                return null;
            float widthRatio = originalWidth / targetWidth;
            float heightRatio = originalHeight / targetHeight;
            //计算压缩值
            float ratio = widthRatio > heightRatio ? widthRatio : heightRatio;
            if (ratio < 1)
                ratio = 1;

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = (int) ratio;
            bitmapOptions.inDither = true;
            bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            input = context.getContentResolver().openInputStream(uri);
            //实际获取图片
            bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            if (input != null) {
                input.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return bitmap;
    }



    /**
     * 质量压缩方法
     *
     * @param image
     * @return
     */
    public static byte[] compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
            if(options <= 0){
                break;
            }
        }

        return baos.toByteArray();
    }


    public static byte[] WeChatBitmapToByteArray(Bitmap bmp) {

        // 首先进行一次大范围的压缩

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        float zoom = (float) Math.sqrt(32 * 1024 / (float)output.toByteArray().length); //获取缩放比例

        // 设置矩阵数据
        Matrix matrix = new Matrix();
        matrix.setScale(zoom, zoom);

        // 根据矩阵数据进行新bitmap的创建
        Bitmap resultBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        output.reset();

        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

        // 如果进行了上面的压缩后，依旧大于32K，就进行小范围的微调压缩
        while(output.toByteArray().length > 32 * 1024){
            matrix.setScale(0.9f, 0.9f);//每次缩小 1/10

            resultBitmap = Bitmap.createBitmap(
                    resultBitmap, 0, 0,
                    resultBitmap.getWidth(), resultBitmap.getHeight(), matrix,true);

            output.reset();
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        }

        return output.toByteArray();
    }


}
