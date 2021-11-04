package com.android.calculator2.db;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Created by phatnd on 17,May,2019
 * Vintech, Vietnam
 */
public class SystemSettingsResolver {

    public static final String OEM_BLACK_MODE = "oem_black_mode";
    public static final String OEM_BLACK_MODE_ACCENT_COLOR = "oem_black_mode_accent_color";
    public static final String OEM_BLACK_MODE_ACCENT_COLOR_INDEX = "oem_black_mode_accent_color_index";


    private Context context;

    public SystemSettingsResolver(Context context) {
        this.context = context;
    }

    public int getInt(String setting) {
        ContentResolver resolver = context.getContentResolver();
        try {
            return Settings.System.getInt(resolver, setting);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getString(String setting) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.System.getString(resolver, setting);
    }
}