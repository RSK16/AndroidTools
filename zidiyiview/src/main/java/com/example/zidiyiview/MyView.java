package com.example.zidiyiview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

/**
 * Created by apple on 16/8/1.
 */

/**
 * 自定义控件 实现一个带文字的图片（图片。文字使用ondraw实现）
 */
public class MyView extends View {
    private String mtext;
    private int msrc;
    public MyView(Context context) {
        super(context);
    }

    public MyView(Context context, AttributeSet attrs) {
        //this(context, attrs, 0);
        super(context, attrs);
        int resourceId = 0;
        int textId = attrs.getAttributeResourceValue(null, "Text", 0);
        int srcId = attrs.getAttributeResourceValue(null, "Src", 0);
        mtext = context.getResources().getText(textId).toString();
        msrc=srcId;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        InputStream is = getResources().openRawResource(msrc);
        Bitmap mbitmap = BitmapFactory.decodeStream(is);
        int bh = mbitmap.getHeight();
        int bw = mbitmap.getWidth();
        canvas.drawBitmap(mbitmap, 0, 0, paint);
        canvas.drawText(mtext, bw / 2, 30, paint);
    }
}
