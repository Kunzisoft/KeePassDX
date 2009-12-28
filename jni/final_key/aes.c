#include <openssl/evp.h>
#include <openssl/aes.h>
#include <jni.h>
#include <stdlib.h>

#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "KeePassDroidNative", __VA_ARGS__)
#define LOGD(...) 
//#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "KeePassDroidNative", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "KeePassDroidNative", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "KeePassDroidNative", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "KeePassDroidNative", __VA_ARGS__) 

jlong Java_com_keepassdroid_crypto_NativeAESCipherSpi_nativeInit(JNIEnv *env, 
        jobject this, jboolean encrypt, jbyteArray key, jbyteArray iv,
        jboolean padding) {

    LOGD("1");
    // Convert keys to c
    jsize key_len = (*env)->GetArrayLength(env, key);
    char *c_key = (char *) malloc(key_len);
    (*env)->GetByteArrayRegion(env, key, 0, key_len, c_key);
    LOGD("2: Keylen: %d", key_len);

    
    // Covert iv to c
    jsize iv_len = (*env)->GetArrayLength(env, iv);
    char *c_iv = (char *) malloc(iv_len);
    (*env)->GetByteArrayRegion(env, iv, 0, iv_len, c_iv);
    LOGD("3: IvLen: %d", iv_len);

    EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *) malloc(sizeof(EVP_CIPHER_CTX));
    LOGD("3.5: %d", sizeof(EVP_CIPHER_CTX));

    EVP_CIPHER_CTX_init(ctx);
    EVP_CipherInit_ex(ctx, EVP_aes_256_cbc(), NULL, c_key, c_iv, encrypt);

    LOGD("4");
    if ( padding ) {
        EVP_CIPHER_CTX_set_padding(ctx, 1);
    } else {
        EVP_CIPHER_CTX_set_padding(ctx, 0);
    }

    LOGD("5");
    // Free allocated memory
    free(c_iv);
    free(c_key);

    LOGD("6: ctxPtr=%d",ctx);

    return (jlong) ctx;
}

void Java_com_keepassdroid_crypto_NativeAESCipherSpi_nativeCleanup(JNIEnv *env, 
        jclass this, jlong ctxPtr) {

    LOGD("cleanup");
    EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *) ctxPtr;

    EVP_CIPHER_CTX_cleanup(ctx);
    free(ctx);
}

jint Java_com_keepassdroid_crypto_NativeAESCipherSpi_nativeUpdate(JNIEnv *env, 
        jobject this, jlong ctxPtr, jbyteArray input, jint inputOffset, 
        jint inputLen, jbyteArray output, jint outputOffset, jint outputSize) {


    LOGD("InputSize: %d; OutputSize: %d", inputLen, outputSize);
    if ( inputLen == 0 ) {
        return 0;
    }

    char *c_input = (char *) malloc(inputLen);

    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, c_input);

    int outLen;
    char *c_output;

    // Worst case is all full blocks with 1 byte on the left and right
    //int max_update_size = (((inputLen - 2) / AES_BLOCK_SIZE) + 2) * AES_BLOCK_SIZE;
    c_output = (char *) malloc(outputSize);

    EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *) ctxPtr;
    LOGD("Pre: ctxPtr=%d", ctx);
    EVP_CipherUpdate(ctx, c_output, &outLen, c_input, inputLen);
    LOGD("Post");

    /* output can differ on final 
    if ( outLen != ((int) outputSize) ) {
        LOGD("Outsize differs: %d", outLen);
        free(c_output);
        free(c_input);
        return -1;
    }
    */

    LOGD("PreOut: OutLen=%d", outLen);
    (*env)->SetByteArrayRegion(env, output, outputOffset, outLen, c_output);

    free(c_output);
    free(c_input);
    LOGD("PostOut");



    return (jint) outLen; // I think jint should always be bigger than int

}

jint Java_com_keepassdroid_crypto_NativeAESCipherSpi_nativeDoFinal(JNIEnv *env,
        jobject this, jlong ctxPtr, jbyteArray output, jint outputOffset, 
        jint outputSize) {

    LOGD("outputOffset=%d", outputOffset);
    char *c_output = (char *) malloc(outputSize);

    int outLen;
    EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *) ctxPtr;
    EVP_CipherFinal_ex(ctx, c_output, &outLen);

    /*
    if ( outLen != ((int) outputSize) ) {
        free(c_output);
        return -1;
    }
    */
    LOGD("Final: OutputLen=%d, outputOffset=%d", outLen, (int)outputOffset);

    (*env)->SetByteArrayRegion(env, output, outputOffset, outLen, c_output);

    free(c_output);

    return (jint) outLen;

}
