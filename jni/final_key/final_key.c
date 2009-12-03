#include <openssl/evp.h>
#include <openssl/aes.h>
#include <jni.h>
#include <string.h>

#define BLOCK_SIZE AES_BLOCK_SIZE * 2

/* Prepare the final key */
jbyteArray Java_com_keepassdroid_crypto_finalkey_NativeFinalKey_nativeTransformMasterKey(JNIEnv *env, jobject this, 
        jbyteArray seed, jbyteArray key, jint rounds) {

    char cSeed[BLOCK_SIZE];
    char cKey[BLOCK_SIZE];
    char destKey[BLOCK_SIZE];

    // Verify length of key and seed
    if ( (*env)->GetArrayLength(env, seed) != BLOCK_SIZE ) {
        return NULL;
    }
    if ( (*env)->GetArrayLength(env, key) != BLOCK_SIZE ) {
        return NULL;
    }

    // TODO: Test to see if GetByteArrayElements is cheaper
    (*env)->GetByteArrayRegion(env, seed, 0, BLOCK_SIZE, cSeed);
    (*env)->GetByteArrayRegion(env, key, 0, BLOCK_SIZE, cKey);

    EVP_CIPHER_CTX ctx;

    EVP_CIPHER_CTX_init(&ctx);
    EVP_EncryptInit_ex(&ctx, EVP_aes_256_ecb(), NULL, cSeed, NULL);

    int c_len; // Not really needed, we always work at even block sizes here
    int i;
    for (i = 0; i < rounds; i++) {
            EVP_EncryptUpdate(&ctx, destKey, &c_len, cKey, BLOCK_SIZE);
            // TODO: I could get fancy and avoid this copy
            memcpy(cKey, destKey, BLOCK_SIZE);
    }

    EVP_CIPHER_CTX_cleanup(&ctx);

    jbyteArray result = (*env)->NewByteArray(env, BLOCK_SIZE);
    (*env)->SetByteArrayRegion(env, result, 0, BLOCK_SIZE, destKey);

    return result;
}

/* For testing purposes only */
jbyteArray Java_com_keepassdroid_crypto_finalkey_NativeFinalKey_nativeReflect(JNIEnv *env, jclass this, 
        jbyteArray key) {
    
    return key;
}
