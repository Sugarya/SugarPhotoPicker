package com.sugarya.sugarphotopicker.widget.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.sugarya.sugarphotopicker.R;

/**
 * 图片加载器工具类
 * Created by Ethan on 2017/7/11.
 */

public class ImageLoader {

    public static void display(Context context, String url, ImageView imageView){
        if(context == null || imageView == null){
            return;
        }
        if (TextUtils.isEmpty(url)) {
            url = "http://";
        }

        Picasso.with(context)
                .load(url)
//                .placeholder(R.drawable.icon_image_holder)
//                .error(R.drawable.icon_image_holder)
                .into(imageView);
    }

    public static void display(Context context, Uri uri, ImageView imageView, int lengthDip){
        if(context == null || imageView == null || uri == null){
            return;
        }

        Picasso.with(context)
                .load(uri)
//                .placeholder(R.drawable.icon_image_holder)
//                .error(R.drawable.icon_image_holder)
                .resize(dip2px(context, lengthDip), dip2px(context,lengthDip))
                .into(imageView);
    }

    public static void display(Context context, String url, Target target){
        if(context == null){
            return;
        }
        if (TextUtils.isEmpty(url)) {
            url = "http://";
        }
        Picasso.with(context)
                .load(url)
//                .placeholder(R.drawable.icon_image_holder)
//                .error(R.drawable.icon_image_holder)
                .into(target);
    }

    private static int dip2px(Context context, float dipValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

}
