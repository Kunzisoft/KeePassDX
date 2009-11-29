LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libgpg-error

LOCAL_SRC_FLES := code-from-errno.c \
    code-to-errno.c \
    init.c \
    strsource.c \
    strerror.c

include $(BUILD_SHARED_LIBRARY)
