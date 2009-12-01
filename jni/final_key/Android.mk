LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := final-key

LOCAL_SRC_FILES := final_key.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libgcrypt/src $(LOCAL_PATH)/../libgpg-error/src/ #$(LOCAL_PATH)/../mpi

LOCAL_SHARED_LIBRARIES := libgpg-error libgcrypt 

include $(BUILD_SHARED_LIBRARY)
