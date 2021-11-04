package com.android.calculator2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/*******************************************************************************
 * Copyright © 2019 VSmart JSC. All rights reserved.
 * Created date: 04/24/19 9:14 AM
 * Author: thinhlh1
 * Brief:  VCalculator
 ******************************************************************************/
public class ZeroButton extends Button {

    public ZeroButton(Context context) {
        super(context);
    }

    public ZeroButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZeroButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * calculator a min size to create a square view
     */
//    @Override
//    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        final int width = MeasureSpec.getSize(widthMeasureSpec);
//        final int height = MeasureSpec.getSize(heightMeasureSpec);
//        float padding = getResources().getDimension(R.dimen.circle_button_padding);
//        int w = width;
//        int h = height;
//        int parentW = (int) (w + padding * 2);
//        int parentH = (int) (h + padding * 2);
//        if (parentH * 2 < parentW) {
//            int marginW = (parentW - h*2)/4;
//            w = parentW - marginW *2;
//        } else if (parentH * 2 > parentW) {
//            h = (int) (parentW / 2 - padding * 2);
//        }
//        super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
//    }
}
