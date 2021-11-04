package com.android.calculator2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.gridlayout.widget.GridLayout;

public class GridLayoutDragable extends GridLayout {

    private GridLayoutDragableListener mListener;
    private float downYValue;

    public interface GridLayoutDragableListener {
        void onScrollDown();
    }

    public GridLayoutDragable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GridLayoutDragable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollListener(GridLayoutDragableListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downYValue = event.getRawY();
                break;
            }
            case MotionEvent.ACTION_UP: {
                float currentY = event.getRawY();
                if (Math.abs(currentY - downYValue) > 200) {
                    if (downYValue < currentY) {
                        if (mListener != null) {
                            mListener.onScrollDown();
                        }
                        return true;
                    }
                }
                break;
            }
        }
        return super.onInterceptTouchEvent(event);
    }
}
