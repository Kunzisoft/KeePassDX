LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := final-key

LOCAL_SRC_FILES := \
	kpd_jni.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../sha $(LOCAL_PATH)/../aes

LOCAL_STATIC_LIBRARIES := aes sha

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
