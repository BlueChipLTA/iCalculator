LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := VinCalculator
LOCAL_SRC_FILES := APK/ExactCalculator.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := ExactCalculator CalculatorGoogle
LOCAL_MODULE_SUFFIX := .apk
include $(BUILD_PREBUILT)