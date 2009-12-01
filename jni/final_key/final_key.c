#include <jni.h>
#include <gcrypt.h>


jbyteArray Java_com_keepassdroid_crypto_FinalKey_nativeTransformMasterKey(JNIEnv *env, jclass this, 
        jbyteArray seed, jbyteArray key, jint rounds) {

    gcry_error_t error;
    gcry_cipher_hd_t *cipher;

    error = _gcry_cipher_open(cipher, GCRY_CIPHER_AES, GCRY_CIPHER_MODE_ECB, 0);
    
    return key;
}

jbyteArray Java_com_keepassdroid_crypto_FinalKey_nativeReflect(JNIEnv *env, jclass this, 
        jbyteArray key) {
    
    return key;
}
