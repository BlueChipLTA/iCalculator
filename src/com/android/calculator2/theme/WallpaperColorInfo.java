package com.android.calculator2.theme;

import android.annotation.TargetApi;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.systemui.shared.system.TonalCompat;
import com.android.systemui.shared.system.TonalCompat.ExtractionInfo;

import java.util.ArrayList;

import static android.app.WallpaperManager.FLAG_SYSTEM;

@TargetApi(Build.VERSION_CODES.P)
public class WallpaperColorInfo implements WallpaperManager.OnColorsChangedListener {

    private static final String TAG = "WallpaperColorInfo";
    private static final Object sInstanceLock = new Object();
    private static WallpaperColorInfo sInstance;
    private AsyncTask<Boolean, Boolean, Boolean> mLoadWallpagerColorTask = null;

    public static WallpaperColorInfo getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new WallpaperColorInfo(context.getApplicationContext());
            }
            return sInstance;
        }
    }

    public static WallpaperColorInfo getInstance() {
        return sInstance;
    }

    public static boolean isDarkMode() {
        if (sInstance != null) {
            return sInstance.isDark();
        } else {
            return false;
        }
    }

    private final ArrayList<OnChangeListener> mListeners = new ArrayList<>();
    private final WallpaperManager mWallpaperManager;
    private final TonalCompat mTonalCompat;

    private ExtractionInfo mExtractionInfo;

    private OnChangeListener[] mTempListeners = new OnChangeListener[0];

    private WallpaperColorInfo(Context context) {
        mWallpaperManager = context.getSystemService(WallpaperManager.class);
        mTonalCompat = new TonalCompat(context);

        mWallpaperManager.addOnColorsChangedListener(this, new Handler(Looper.getMainLooper()));
    }

    public boolean isDark() {
        if (mExtractionInfo != null) {
            return mExtractionInfo.supportsDarkTheme;
        }
        return false;
    }

    @Override
    public void onColorsChanged(WallpaperColors colors, int which) {
        if (colors != null && (which & FLAG_SYSTEM) != 0) {
            update(colors);
            notifyChange();
        }
    }

    public WallpaperColorInfo initExtrationInfo() {
        if (mExtractionInfo != null) {
            return this;
        }
        if (mLoadWallpagerColorTask != null) {
            mLoadWallpagerColorTask.cancel(true);
        }
        mLoadWallpagerColorTask = new AsyncTask<Boolean, Boolean, Boolean>() {
            @Override
            protected Boolean doInBackground(Boolean... booleans) {
                update(mWallpaperManager.getWallpaperColors(FLAG_SYSTEM));
                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                mLoadWallpagerColorTask = null;
                if (mExtractionInfo != null) {
                    notifyChange();
                }
            }
        };
        mLoadWallpagerColorTask.execute();
        return this;
    }


    private void update(WallpaperColors wallpaperColors) {
        mExtractionInfo = mTonalCompat.extractDarkColors(wallpaperColors);
    }

    public void addOnChangeListener(OnChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeOnChangeListener(OnChangeListener listener) {
        mListeners.remove(listener);
        Log.d(TAG, "removeOnChangeListener");
    }

    public <T> void removeOnChangeListener(Class<T> classType) {
        for (OnChangeListener ac : mListeners) {
            if (classType.isInstance(ac)) {
                mListeners.remove(ac);
            }
        }
    }

    private void notifyChange() {
        // Create a new array to avoid concurrent modification when the activity destroys itself.
        mTempListeners = mListeners.toArray(mTempListeners);
        for (OnChangeListener listener : mTempListeners) {
            if (listener != null) {
                listener.onExtractedColorsChanged(this);
            }
        }
        //leak after notify
        mTempListeners = new OnChangeListener[0];
    }

    public interface OnChangeListener {
        default void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
            Log.d(TAG, "onExtractedColorsChanged: ");
        }
    }
}