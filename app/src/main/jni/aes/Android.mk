LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := aes

LOCAL_SRC_FILES := \
	aescrypt.c \
	aeskey.c \
	aes_modes.c \
	aestab.c

include $(BUILD_STATIC_LIBRARY)
