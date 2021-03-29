LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sha

LOCAL_SRC_FILES := \
	sha1.c \
	sha2.c \
	hmac.c

LOCAL_CFLAGS := -DUSE_SHA256

include $(BUILD_STATIC_LIBRARY)
