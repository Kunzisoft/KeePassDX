#include <openssl/evp.h>
#include <openssl/aes.h>
#include <jni.h>
#include <string.h>

#define BLOCK_SIZE AES_BLOCK_SIZE * 2

/* Prepare the final key */
jbyteArray Java_com_keepassdroid_crypto_finalkey_NativeFinalKey_nativeTransformMasterKey(JNIEnv *env, jobject this, 
        jbyteArray seed, jbyteArray key, jint rounds) {

    char cSeed[BLOCK_SIZE];
    char key1[BLOCK_SIZE];
    char key2[BLOCK_SIZE];

    // Verify length of key and seed
    if ( (*env)->GetArrayLength(env, seed) != BLOCK_SIZE ) {
        return NULL;
    }
    if ( (*env)->GetArrayLength(env, key) != BLOCK_SIZE ) {
        return NULL;
    }

    // TODO: Test to see if GetByteArrayElements is cheaper
    (*env)->GetByteArrayRegion(env, seed, 0, BLOCK_SIZE, cSeed);
    (*env)->GetByteArrayRegion(env, key, 0, BLOCK_SIZE, key1);

    EVP_CIPHER_CTX ctx;

    EVP_CIPHER_CTX_init(&ctx);
    EVP_EncryptInit_ex(&ctx, EVP_aes_256_ecb(), NULL, cSeed, NULL);

    int c_len; // Not really needed, we always work at even block sizes here
    int i;
    char flip = 0;
    for (i = 0; i < rounds; i++) {
        // Toggle between arrays as source and destination
        if ( flip ) {
            EVP_EncryptUpdate(&ctx, key1, &c_len, key2, BLOCK_SIZE);
            flip = 0;
        } else {
            EVP_EncryptUpdate(&ctx, key2, &c_len, key1, BLOCK_SIZE);
            flip = 1;
        }
    }

    EVP_CIPHER_CTX_cleanup(&ctx);

    jbyteArray result = (*env)->NewByteArray(env, BLOCK_SIZE);


    // Need to take the SHA1 digest
    EVP_MD_CTX digestCtx;
    EVP_MD_CTX_init(&digestCtx);

    EVP_DigestInit_ex(&digestCtx, EVP_sha256(), NULL);

    if ( flip ) {
        EVP_DigestUpdate(&digestCtx, key2, BLOCK_SIZE);
        EVP_DigestFinal_ex(&digestCtx, key1, NULL);
        flip = 0;
    } else {
        EVP_DigestUpdate(&digestCtx, key1, BLOCK_SIZE);
        EVP_DigestFinal_ex(&digestCtx, key2, NULL);
        flip = 1;
    }

    EVP_MD_CTX_cleanup(&digestCtx);

    if ( flip ) {
        (*env)->SetByteArrayRegion(env, result, 0, BLOCK_SIZE, key2);
    } else {
        (*env)->SetByteArrayRegion(env, result, 0, BLOCK_SIZE, key1);
    }

    return result;
}

/* For testing purposes only */
jbyteArray Java_com_keepassdroid_crypto_finalkey_NativeFinalKey_nativeReflect(JNIEnv *env, jclass this, 
        jbyteArray key) {
    
    return key;
}
