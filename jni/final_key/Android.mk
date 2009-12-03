LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := final-key

LOCAL_SRC_FILES := final_key.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../openssl-0.9.8l/include

LOCAL_STATIC_LIBRARIES := openssl-crypto

include $(BUILD_SHARED_LIBRARY)
