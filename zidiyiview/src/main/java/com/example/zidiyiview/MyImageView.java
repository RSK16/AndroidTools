package com.example.zidiyiview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by apple on 16/8/1.
 */
public class MyImageView extends LinearLayout {
    public MyImageView(Context context) {
        super(context);
    }

    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int resourceId = -1;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyImageView);
        ImageView iv = new ImageView(context);
        TextView tv = new TextView(context);
        int N = typedArray.getIndexCount();
        for (int i =0;i<N ;i++){
            int attr = typedArray.getIndex(i);
            switch (attr){
                case R.styleable.MyImageView_Oriental:
                    resourceId = typedArray.getInt(R.styleable.MyImageView_Oriental, 0);
                    this.setOrientation(resourceId == 1 ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
                    break;
                case R.styleable.MyImageView_Text:
                    resourceId = typedArray.getInt(R.styleable.MyImageView_Text, 0);
                    tv.setText(resourceId > 0 ? typedArray.getResources().getText(resourceId)
                            :typedArray.getString(R.styleable.MyImageView_Text));
                    break;
                case R.styleable.MyImageView_Src:
                    resourceId = typedArray.getInt(R.styleable.MyImageView_Src, 0);
                    iv.setImageResource(resourceId > 0 ? resourceId : R.mipmap.ic_launcher);
                    break;
            }
            addView(iv);
            addView(tv);
            typedArray.recycle();
        }
    }
}
