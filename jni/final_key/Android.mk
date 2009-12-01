LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := final-key

LOCAL_SRC_FILES := final_key.c

include $(BUILD_SHARED_LIBRARY)
