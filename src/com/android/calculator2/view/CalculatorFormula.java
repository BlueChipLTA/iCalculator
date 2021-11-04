/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.view;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.calculator2.CalculatorActivity;
import com.android.calculator2.R;
import com.android.calculator2.expression.Evaluator;
import com.android.calculator2.util.KeyMaps;
import com.android.calculator2.util.StringUtils;

/**
 * TextView adapted for displaying the formula and allowing pasting.
 */
public class CalculatorFormula extends AlignedEditView implements MenuItem.OnMenuItemClickListener,
        ClipboardManager.OnPrimaryClipChangedListener {

    public static final String TAG_ACTION_MODE = "ACTION_MODE";

    // Temporary paint for use in layout methods.
    private final TextPaint mTempPaint = new TextPaint();

    private final float mMaximumTextSize;
    private final float mMinimumTextSize;
    private final float mStepTextSize;

    private final ClipboardManager mClipboardManager;

    private Evaluator mEvaluator;
    private int mWidthConstraint = -1;
    private ActionMode mActionMode;
    private ActionMode.Callback mInsertActionModeCallback;
    private ActionMode.Callback mSelectionActionModeCallback;
    private ContextMenu mContextMenu;
    private OnTextSizeChangeListener mOnTextSizeChangeListener;
    private OnFormulaContextMenuClickListener mOnContextMenuClickListener;
    private CalculatorActivity.OnDisplayMemoryOperationsListener mOnDisplayMemoryOperationsListener;

    public CalculatorFormula(Context context) {
        this(context, null /* attrs */);
    }

    public CalculatorFormula(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public CalculatorFormula(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CalculatorFormula, defStyleAttr, 0);
        mMaximumTextSize = a.getDimension(
                R.styleable.CalculatorFormula_maxTextSize, getTextSize());
        mMinimumTextSize = a.getDimension(
                R.styleable.CalculatorFormula_minTextSize, getTextSize());
        mStepTextSize = a.getDimension(R.styleable.CalculatorFormula_stepTextSize,
                (mMaximumTextSize - mMinimumTextSize) / 3);
        a.recycle();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupActionMode();
        } else {
            setupContextMenu();
        }

        setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setShowSoftInputOnFocus(false);
        setCursorVisible(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!isLaidOut()) {
            // Prevent shrinking/resizing with our variable textSize.
            setTextSizeInternal(TypedValue.COMPLEX_UNIT_PX, mMaximumTextSize,
                    false /* notifyListener */);
            setMinimumHeight(getLineHeight() + getCompoundPaddingBottom()
                    + getCompoundPaddingTop());
        }

        // Ensure we are at least as big as our parent.
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        if (getMinimumWidth() != width) {
            setMinimumWidth(width);
        }

        // Re-calculate our textSize based on new width.
        mWidthConstraint = MeasureSpec.getSize(widthMeasureSpec)
                - getPaddingLeft() - getPaddingRight();
        final float textSize = getVariableTextSize(getText());
        if (getTextSize() != textSize) {
            setTextSizeInternal(TypedValue.COMPLEX_UNIT_PX, textSize, false /* notifyListener */);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mClipboardManager.addPrimaryClipChangedListener(this);
        onPrimaryClipChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mClipboardManager.removePrimaryClipChangedListener(this);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, getVariableTextSize(text.toString()));
    }

    private void setTextSizeInternal(int unit, float size, boolean notifyListener) {
        final float oldTextSize = getTextSize();
        super.setTextSize(unit, size);
        if (notifyListener && mOnTextSizeChangeListener != null && getTextSize() != oldTextSize) {
            mOnTextSizeChangeListener.onTextSizeChanged(this, oldTextSize);
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        setTextSizeInternal(unit, size, true);
    }

    public float getMinimumTextSize() {
        return mMinimumTextSize;
    }

    public float getMaximumTextSize() {
        return mMaximumTextSize;
    }

    public float getVariableTextSize(CharSequence text) {
        if (mWidthConstraint < 0 || mMaximumTextSize <= mMinimumTextSize) {
            // Not measured, bail early.
            return getTextSize();
        }

        // Capture current paint state.
        mTempPaint.set(getPaint());

        // Step through increasing text sizes until the text would no longer fit.
        float lastFitTextSize = mMinimumTextSize;
        while (lastFitTextSize < mMaximumTextSize) {
            mTempPaint.setTextSize(Math.min(lastFitTextSize + mStepTextSize, mMaximumTextSize));
            if (Layout.getDesiredWidth(text, mTempPaint) > mWidthConstraint) {
                break;
            }
            lastFitTextSize = mTempPaint.getTextSize();
        }

        return lastFitTextSize;
    }

    /**
     * Functionally equivalent to setText(), but explicitly announce changes.
     * If the new text is an extension of the old one, announce the addition.
     * Otherwise, e.g. after deletion, announce the entire new text.
     */
    public void changeTextTo(CharSequence newText) {
        // get current selection
        final int start = getSelectionStart();
        final CharSequence oldText = getText();
        final char separator = KeyMaps.translateResult(",").charAt(0);
        final CharSequence added = StringUtils.getExtensionIgnoring(newText, oldText, separator);
        if (added != null) {
            if (added.length() == 1) {
                // The algorithm for pronouncing a single character doesn't seem
                // to respect our hints.  Don't give it the choice.
                final char c = added.charAt(0);
                final int id = KeyMaps.keyForChar(c);
                final String descr = KeyMaps.toDescriptiveString(getContext(), id);
                if (descr != null) {
                    announceForAccessibility(descr);
                } else {
                    announceForAccessibility(String.valueOf(c));
                }
            } else if (added.length() != 0) {
                announceForAccessibility(added);
            }
        } else {
            announceForAccessibility(newText);
        }
        setText(newText, BufferType.SPANNABLE);

        // Get current cursor to set on formula
        int cursor;
        if (newText.length() > oldText.length()) {
            cursor = start + (newText.length() - oldText.length());
        } else {
            cursor = start - (oldText.length() - newText.length());
        }
        if (cursor < 0) {
            cursor = 0;
        } else if (cursor > getText().toString().length()) {
            cursor = getText().toString().length();
        }
        setSelection(cursor);
    }

    public boolean stopActionModeOrContextMenu() {
        if (mActionMode != null) {
            mActionMode.finish();
            return true;
        }
        if (mContextMenu != null) {
            mContextMenu.close();
            return true;
        }
        return false;
    }

    public void changeTextSelection(CharSequence newText,int index) {
        final CharSequence oldText = getText();
        final char separator = KeyMaps.translateResult(",").charAt(0);
        final CharSequence added = StringUtils.getExtensionIgnoring(newText, oldText, separator);
        if (added != null) {
            if (added.length() == 1) {
                // The algorithm for pronouncing a single character doesn't seem
                // to respect our hints.  Don't give it the choice.
                final char c = added.charAt(0);
                final int id = KeyMaps.keyForChar(c);
                final String descr = KeyMaps.toDescriptiveString(getContext(), id);
                if (descr != null) {
                    announceForAccessibility(descr);
                } else {
                    announceForAccessibility(String.valueOf(c));
                }
            } else if (added.length() != 0) {
                announceForAccessibility(added);
            }
        } else {
            announceForAccessibility(newText);
        }
        setText(newText, BufferType.SPANNABLE);
        if (newText.toString().length() >= index){
            setSelection(index);
        }
    }

    public boolean isSelectionMode(){
        if (mActionMode != null || mContextMenu != null){
            return true;
        }
        return false;
    }

    public void setEvaluator(Evaluator evaluator) {
        mEvaluator = evaluator;
    }

    public void setOnTextSizeChangeListener(OnTextSizeChangeListener listener) {
        mOnTextSizeChangeListener = listener;
    }

    public void setOnContextMenuClickListener(OnFormulaContextMenuClickListener listener) {
        mOnContextMenuClickListener = listener;
    }

    public void setOnDisplayMemoryOperationsListener(
            CalculatorActivity.OnDisplayMemoryOperationsListener listener) {
        mOnDisplayMemoryOperationsListener = listener;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (getSelectionStart() != getSelectionEnd()) {
            setSelection(getText().length());
        }
    }


    /**
     * Use ActionMode for paste support on M and higher.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void setupActionMode() {
        mSelectionActionModeCallback = new ActionMode.Callback2() {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == android.R.id.selectAll) {
                    selectAll();
                    return true;
                } else if (onMenuItemClick(item)) {
                    mode.finish();
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTag(TAG_ACTION_MODE);
                mActionMode = mode;
                final MenuInflater inflater = mode.getMenuInflater();
                return createSelectionContextMenu(inflater, menu);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // update menu
                final MenuItem memoryStoreItem = menu.findItem(R.id.memory_store);
                final MenuItem memoryAddItem = menu.findItem(R.id.memory_add);
                final MenuItem memorySubtractItem = menu.findItem(R.id.memory_subtract);
                final String selection = getSelectionText();
                if (StringUtils.isNumber(selection)) {
                    memoryStoreItem.setEnabled(true);
                    final boolean displayMemory = mEvaluator.isMemory();
                    memoryAddItem.setEnabled(displayMemory);
                    memorySubtractItem.setEnabled(displayMemory);
                } else {
                    memoryStoreItem.setEnabled(false);
                    memoryAddItem.setEnabled(false);
                    memorySubtractItem.setEnabled(false);
                }
                final MenuItem menuTranslate = menu.findItem(0);
                if (menuTranslate != null) {
                    menuTranslate.setVisible(false);
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }

            @Override
            public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
                super.onGetContentRect(mode, view, outRect);
                outRect.top += getTotalPaddingTop();
                outRect.right -= getTotalPaddingRight();
                outRect.bottom -= getTotalPaddingBottom();
                // Encourage menu positioning over the rightmost 10% of the screen.
                outRect.left = (int) (outRect.right * 0.9f);
            }
        };

        mInsertActionModeCallback = new ActionMode.Callback2() {

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (onMenuItemClick(item)) {
                    mode.finish();
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTag(TAG_ACTION_MODE);
                mActionMode = mode;
                final MenuInflater inflater = mode.getMenuInflater();
                return createInsertContextMenu(inflater, menu);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }

            @Override
            public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
                super.onGetContentRect(mode, view, outRect);
                outRect.top += getTotalPaddingTop();
                outRect.right -= getTotalPaddingRight();
                outRect.bottom -= getTotalPaddingBottom();
                // Encourage menu positioning over the rightmost 10% of the screen.
                outRect.left = (int) (outRect.right * 0.9f);
            }
        };

        setCustomSelectionActionModeCallback(mSelectionActionModeCallback);
        setCustomInsertionActionModeCallback(mInsertActionModeCallback);
    }

    /**
     * Use ContextMenu for paste support on L and lower.
     */
    private void setupContextMenu() {
        setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                            ContextMenu.ContextMenuInfo contextMenuInfo) {
                final MenuInflater inflater = new MenuInflater(getContext());
                createInsertContextMenu(inflater, contextMenu);
                mContextMenu = contextMenu;
                for (int i = 0; i < contextMenu.size(); i++) {
                    contextMenu.getItem(i).setOnMenuItemClickListener(CalculatorFormula.this);
                }
            }
        });
        setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
    }

    private boolean createSelectionContextMenu(MenuInflater inflater, Menu menu) {
        bringPointIntoView(length());
        inflater.inflate(R.menu.menu_formula_selection, menu);
        if (menu != null && menu.size() > 0) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                item.setEnabled(false);
            }
        }

        final MenuItem memoryStoreItem = menu.findItem(R.id.memory_store);
        final MenuItem memoryAddItem = menu.findItem(R.id.memory_add);
        final MenuItem memorySubtractItem = menu.findItem(R.id.memory_subtract);

        final MenuItem cutAddItem = menu.findItem(android.R.id.cut);
        final MenuItem copyAddItem = menu.findItem(android.R.id.copy);
        final MenuItem pasteAddItem = menu.findItem(android.R.id.paste);
        final MenuItem shareAddItem = menu.findItem(android.R.id.shareText);
        final MenuItem selectAllItem = menu.findItem(android.R.id.selectAll);

        if (cutAddItem != null)
            cutAddItem.setEnabled(true);
        if (copyAddItem != null)
            copyAddItem.setEnabled(true);
        if (pasteAddItem != null)
            pasteAddItem.setEnabled(true);
        if (shareAddItem != null)
            shareAddItem.setEnabled(true);
        if (selectAllItem != null)
            selectAllItem.setEnabled(true);

        final String selection = getSelectionText();
        if (StringUtils.isNumber(selection)) {
            memoryStoreItem.setEnabled(true);
            final boolean displayMemory = mEvaluator.isMemory();
            memoryAddItem.setEnabled(displayMemory);
            memorySubtractItem.setEnabled(displayMemory);
        } else {
            memoryStoreItem.setEnabled(false);
            memoryAddItem.setEnabled(false);
            memorySubtractItem.setEnabled(false);
        }

        return true;
    }

    private boolean createInsertContextMenu(MenuInflater inflater, Menu menu) {
        bringPointIntoView(length());
        inflater.inflate(R.menu.menu_formula, menu);
        if (menu != null && menu.size() > 0) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                item.setEnabled(false);
            }
        }

        final MenuItem cutAddItem = menu.findItem(android.R.id.cut);
        final MenuItem copyAddItem = menu.findItem(android.R.id.copy);
        final MenuItem pasteAddItem = menu.findItem(android.R.id.paste);

        if (cutAddItem != null)
            cutAddItem.setEnabled(true);
        if (copyAddItem != null)
            copyAddItem.setEnabled(true);
        if (pasteAddItem != null)
            pasteAddItem.setEnabled(true);

        final MenuItem memoryRecallItem = menu.findItem(R.id.memory_recall);
        final boolean isMemoryEnabled = mEvaluator.isMemory();
        memoryRecallItem.setEnabled(isMemoryEnabled);
        return true;
    }

    private void paste() {
        final ClipData primaryClip = mClipboardManager.getPrimaryClip();
        if (primaryClip != null && mOnContextMenuClickListener != null) {
            mOnContextMenuClickListener.onPaste(primaryClip);
        }
    }

    private void cut() {
        if (mOnContextMenuClickListener != null) {
            mOnContextMenuClickListener.onCut();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.memory_recall:
                mOnContextMenuClickListener.onMemoryRecall();
                return true;
            case android.R.id.paste:
                paste();
                return true;
            case android.R.id.cut:
                cut();
                return false;
            case R.id.memory_add:
                onMemoryAdd();
                return true;
            case R.id.memory_subtract:
                onMemorySubtract();
                return true;
            case R.id.memory_store:
                onMemoryStore();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
    }

    @Override
    public void onPrimaryClipChanged() {
        //setLongClickable(isPasteEnabled() || isMemoryEnabled());
    }

    public void onMemoryStateChanged() {
        //setLongClickable(isPasteEnabled() || isMemoryEnabled());
    }

    private boolean isMemoryEnabled() {
        return mEvaluator.isMemory();
        //return mOnDisplayMemoryOperationsListener != null
        //        && mOnDisplayMemoryOperationsListener.shouldDisplayMemory();
    }

    private boolean isPasteEnabled() {
        final ClipData clip = mClipboardManager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return false;
        }
        CharSequence clipText = null;
        try {
            clipText = clip.getItemAt(0).coerceToText(getContext());
        } catch (Exception e) {
            Log.i("Calculator", "Error reading clipboard:", e);
        }
        return !TextUtils.isEmpty(clipText);
    }

    /**
     * Store the result for this index if it is available.
     * If it is unavailable, set mStoreToMemoryRequested to indicate that we should store
     * when evaluation is complete.
     */
    public void onMemoryStore() {
        // check selection is number
        //if (mEvaluator.hasResult(Evaluator.MAIN_INDEX)) {
        //    mEvaluator.copyToMemory(Evaluator.MAIN_INDEX);
        //}
        final String selection = getSelectionText();
        mEvaluator.saveMemory(selection);
    }

    /**
     * Add the result to the value currently in memory.
     */
    public void onMemoryAdd() {
        //mEvaluator.addToMemory(Evaluator.MAIN_INDEX);
        final String selection = getSelectionText();
        mEvaluator.addMemory(selection);
    }

    /**
     * Subtract the result from the value currently in memory.
     */
    public void onMemorySubtract() {
        //mEvaluator.subtractFromMemory(Evaluator.MAIN_INDEX);
        final String selection = getSelectionText();
        mEvaluator.minusMemory(selection);
    }

    private String getSelectionText() {
        int start;
        int end;
        if (getSelectionStart() < getSelectionEnd()) {
            start = getSelectionStart();
            end = getSelectionEnd();
        } else {
            start = getSelectionEnd();
            end = getSelectionStart();
        }
        String selection;
        if (start == end) {
            selection = "";
        } else {
            selection = getText().toString().substring(start, end);
        }
        return selection;
    }

    public interface OnTextSizeChangeListener {
        void onTextSizeChanged(TextView textView, float oldSize);
    }

    public interface OnFormulaContextMenuClickListener {
        void onCut();

        boolean onPaste(ClipData clip);

        void onMemoryRecall();
    }
}
