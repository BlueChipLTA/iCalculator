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
public class CircleButton extends Button {

    public CircleButton(Context context) {
        super(context);
    }

    public CircleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * calculator a min size to create a square view
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
    }
}
