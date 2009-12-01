LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libgcrypt-random

LOCAL_SRC_FILES := \
    random.c \
    random-csprng.c \
    random-fips.c \
    rndhw.c \
    rndlinux.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../  $(LOCAL_PATH)/../src $(LOCAL_PATH)/../../libgpg-error/src/ 

include $(BUILD_STATIC_LIBRARY)
