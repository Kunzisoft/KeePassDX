/*
  This is a JNI wrapper for AES & SHA source code on Android.
  Copyright (C) 2010 Michael Mohr

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string.h>
#include <jni.h>

#include "aes.h"
#include "sha2.h"

static JavaVM *cached_vm;
static jclass bad_arg, no_mem;

typedef enum {
  ENCRYPTION,
  DECRYPTION,
  FINALIZED
} edir_t;

typedef struct _aes_encryption_state {
  edir_t direction;
  uint32_t cache_len;
  uint8_t iv[16], cache[16];
  aes_encrypt_ctx ctx[1];
} aes_encryption_state;

typedef struct _aes_decryption_state {
  edir_t direction;
  uint32_t cache_len;
  uint8_t iv[16], cache[16];
  aes_decrypt_ctx ctx[1];
} aes_decryption_state;

JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *vm, void *reserved ) {
  JNIEnv *env;
  jclass cls;

  cached_vm = vm;
  if((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6))
    return JNI_ERR;

  cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if( cls == NULL )
    return JNI_ERR;
  bad_arg = (*env)->NewWeakGlobalRef(env, cls);
  if( bad_arg == NULL )
    return JNI_ERR;

  cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
  if( cls == NULL )
    return JNI_ERR;
  no_mem = (*env)->NewWeakGlobalRef(env, cls);
  if( no_mem == NULL )
    return JNI_ERR;

  aes_init();

  return JNI_VERSION_1_6;
}

// called on garbage collection
JNIEXPORT void JNICALL JNI_OnUnload( JavaVM *vm, void *reserved ) {
  JNIEnv *env;
  if((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6)) {
    return;
  }
  (*env)->DeleteWeakGlobalRef(env, bad_arg);
  (*env)->DeleteWeakGlobalRef(env, no_mem);
  return;
}

JNIEXPORT jlong JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nEncryptInit(JNIEnv *env, jobject this, jbyteArray key, jbyteArray iv) {
  uint8_t ckey[32];
  aes_encryption_state *state;
  jint key_len = (*env)->GetArrayLength(env, key);
  jint iv_len = (*env)->GetArrayLength(env, iv);

  if( ! ( key_len == 16 || key_len == 24 || key_len == 32 ) || iv_len != 16 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid length of key or iv");
    return -1;
  }

  state = (aes_encryption_state *)malloc(sizeof(aes_encryption_state));
  if( state == NULL ) {
    (*env)->ThrowNew(env, no_mem, "Cannot allocate memory for the encryption state");
    return -1;
  }
  memset(state, 0, sizeof(aes_encryption_state));

  (*env)->GetByteArrayRegion(env, key, (jint)0, key_len, (jbyte *)ckey);

  state->direction = ENCRYPTION;
  (*env)->GetByteArrayRegion(env, iv, (jint)0, iv_len, (jbyte *)state->iv);
  aes_encrypt_key(ckey, key_len, state->ctx);

  return (jlong)state;
}

JNIEXPORT jlong JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nDecryptInit(JNIEnv *env, jobject this, jbyteArray key, jbyteArray iv) {
  uint8_t ckey[32];
  aes_decryption_state *state;
  jint key_len = (*env)->GetArrayLength(env, key);
  jint iv_len = (*env)->GetArrayLength(env, iv);

  if( ! ( key_len == 16 || key_len == 24 || key_len == 32 ) || iv_len != 16 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid length of key or iv");
    return -1;
  }

  state = (aes_decryption_state *)malloc(sizeof(aes_decryption_state));
  if( state == NULL ) {
    (*env)->ThrowNew(env, no_mem, "Cannot allocate memory for the decryption state");
    return -1;
  }
  memset(state, 0, sizeof(aes_decryption_state));

  (*env)->GetByteArrayRegion(env, key, (jint)0, key_len, (jbyte *)ckey);

  state->direction = DECRYPTION;
  (*env)->GetByteArrayRegion(env, iv, (jint)0, iv_len, (jbyte *)state->iv);
  aes_decrypt_key(ckey, key_len, state->ctx);

  return (jlong)state;
}

JNIEXPORT void JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nCleanup(JNIEnv *env, jclass this, jlong state) {
  if( state == 0 || state == -1 ) return;
  free((void *)state);
}

/*
  TODO:
  ---Performance--- [low priority]
  Align memory with posix_memalign() (does Android support this?)
*/

JNIEXPORT jint JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nEncryptUpdate(JNIEnv *env, jobject this,
	jlong state, jbyteArray input, jint inputOffset, jint inputLen, jbyteArray output, jint outputOffset, jint outputSize) {
  uint32_t outLen, trailing_bytes, input_plus_cache_len;
  uint8_t *c_input, *c_output;
  aes_encryption_state *c_state;

  // step 1: first, some housecleaning
  if( !inputLen || state <= 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid input length or state");
    return -1;
  }
  c_state = (aes_encryption_state *)state;
  if( c_state->direction != ENCRYPTION ) {
    (*env)->ThrowNew(env, bad_arg, "Decryption state passed to encryption function");
    return -1;
  }
  input_plus_cache_len = inputLen + c_state->cache_len;
  if( input_plus_cache_len < 16 ) {
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)(c_state->cache + c_state->cache_len));
    c_state->cache_len = input_plus_cache_len;
    return 0;
  }
  trailing_bytes = input_plus_cache_len & 15; // mask bottom 4 bits
  outLen = (input_plus_cache_len - trailing_bytes); // output length is now aligned to a 16-byte boundary
  if( outLen > (uint32_t)outputSize ) {
    (*env)->ThrowNew(env, bad_arg, "Output buffer does not have enough space");
    return -1;
  }

  // step 2: allocate memory to hold input and output data
  c_input = (uint8_t *)malloc(input_plus_cache_len);
  if( c_input == NULL ) {
    (*env)->ThrowNew(env, no_mem, "Unable to allocate heap space for encryption input");
    return -1;
  }
  c_output = (uint8_t *)malloc(outLen);
  if( c_output == NULL ) {
    free(c_input);
    (*env)->ThrowNew(env, no_mem, "Unable to allocate heap space for encryption output");
    return -1;
  }

  // step 3: copy data from Java and encrypt it
  if( c_state->cache_len ) {
    memcpy(c_input, c_state->cache, c_state->cache_len);
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)(c_input + c_state->cache_len));
  } else {
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)c_input);
  }
  if( aes_cbc_encrypt(c_input, c_output, outLen, c_state->iv, c_state->ctx) != EXIT_SUCCESS ) {
    free(c_input);
    free(c_output);
    (*env)->ThrowNew(env, bad_arg, "Failed to encrypt input data"); // FIXME: get a better exception class for this...
    return -1;
  }
  (*env)->SetByteArrayRegion(env, output, outputOffset, outLen, (jbyte *)c_output);

  // step 4: cleanup and return
  if( trailing_bytes ) {
    c_state->cache_len = trailing_bytes; // set new cache length
    memcpy(c_state->cache, (c_input + outLen), trailing_bytes); // cache overflow bytes for next call
  } else {
    c_state->cache_len = 0;
  }

  free(c_input);
  free(c_output);

  return outLen;
}

/*
nDecryptUpdate: decrypt a block of data.  The input need not be a multiple of the AES block size.
Parameters:
  state - a saved pointer obtained from a call to nDecryptInit()
  input - an allocated Java byte[] object containing the input data
  inputOffset - encrypted data will be decrypted starting at inputOffset bytes from the beginning of input
  inputLen - a limit on how many bytes will be encrypted from input
  output - analogous to input
  outputOffset - analogous inputOffset
  outputSize - a limit to how many bytes will be accepted for output
*/
JNIEXPORT jint JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nDecryptUpdate(JNIEnv *env, jobject this,
	jlong state, jbyteArray input, jint inputOffset, jint inputLen, jbyteArray output, jint outputOffset, jint outputSize) {
  uint32_t outLen, trailing_bytes, input_plus_cache_len;
  uint8_t *c_input, *c_output;
  aes_decryption_state *c_state;

  // step 1: first, some housecleaning
  if( !inputLen || state <= 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid input length or state");
    return -1;
  }
  c_state = (aes_decryption_state *)state;
  if( c_state->direction != DECRYPTION ) {
    (*env)->ThrowNew(env, bad_arg, "Encryption state passed to decryption function");
    return -1;
  }
  input_plus_cache_len = inputLen + c_state->cache_len;
  if( input_plus_cache_len < 16 ) {
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)(c_state->cache + c_state->cache_len));
    c_state->cache_len = input_plus_cache_len;
    return 0;
  }
  trailing_bytes = input_plus_cache_len & 15; // mask bottom 4 bits
  outLen = (input_plus_cache_len - trailing_bytes); // output length is now aligned to a 16-byte boundary
  if( outLen > (uint32_t)outputSize ) {
    (*env)->ThrowNew(env, bad_arg, "Output buffer does not have enough space");
    return -1;
  }

  // step 2: allocate memory to hold input and output data
  c_input = (uint8_t *)malloc(input_plus_cache_len);
  if( c_input == NULL ) {
    (*env)->ThrowNew(env, no_mem, "Unable to allocate heap space for decryption input");
    return -1;
  }
  c_output = (uint8_t *)malloc(outLen);
  if( c_output == NULL ) {
    free(c_input);
    (*env)->ThrowNew(env, no_mem, "Unable to allocate heap space for decryption output");
    return -1;
  }

  // step 3: copy data from Java and decrypt it
  if( c_state->cache_len ) {
    memcpy(c_input, c_state->cache, c_state->cache_len);
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)(c_input + c_state->cache_len));
  } else {
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)c_input);
  }
  if( aes_cbc_decrypt(c_input, c_output, outLen, c_state->iv, c_state->ctx) != EXIT_SUCCESS ) {
    free(c_input);
    free(c_output);
    (*env)->ThrowNew(env, bad_arg, "Failed to decrypt input data"); // FIXME: get a better exception class for this...
    return -1;
  }
  (*env)->SetByteArrayRegion(env, output, outputOffset, outLen, (jbyte *)c_output);

  // step 4: cleanup and return
  if( trailing_bytes ) {
    c_state->cache_len = trailing_bytes; // set new cache length
    memcpy(c_state->cache, (c_input + outLen), trailing_bytes); // cache overflow bytes for next call
  } else {
    c_state->cache_len = 0;
  }

  free(c_input);
  free(c_output);

  return outLen;
}

/*
nEncryptFinal: encrypt any data remaining in the state's block cache and perform PKCS#5 padding
*/
JNIEXPORT jint JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nEncryptFinal(JNIEnv *env, jobject this,
	jlong state, jboolean doPadding, jbyteArray output, jint outputOffset, jint outputSize) {
  uint32_t pad;
  uint8_t final_output[16] __attribute__ ((aligned (16)));
  aes_encryption_state *c_state;

  if( outputSize < 16 || state <= 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state or outputSize too small");
    return -1;
  }
  c_state = (aes_encryption_state *)state;
  if( c_state->direction != ENCRYPTION ) {
    (*env)->ThrowNew(env, bad_arg, "Cannot finalize the passed state identifier");
    return -1;
  }

  // allow fetching of remaining bytes from cache
  if( !doPadding ) {
    (*env)->SetByteArrayRegion(env, output, outputOffset, c_state->cache_len, (jbyte *)c_state->cache);
    c_state->direction = FINALIZED;
    return c_state->cache_len;
  }

  if( c_state->cache_len ) {
    pad = 16 - c_state->cache_len;
    memset(c_state->cache + c_state->cache_len, pad, pad);
  } else {
    memset(c_state->cache, 0x10, 16);
  }
  if( aes_cbc_encrypt(c_state->cache, final_output, 16, c_state->iv, c_state->ctx) != EXIT_SUCCESS ) {
    (*env)->ThrowNew(env, bad_arg, "Failed to encrypt final data block"); // FIXME: get a better exception class for this...
    return -1;
  }
  (*env)->SetByteArrayRegion(env, output, outputOffset, 16, (jbyte *)final_output);
  c_state->direction = FINALIZED;
  return 16;
}

JNIEXPORT jint JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nDecryptFinal(JNIEnv *env, jobject this,
	jlong state, jboolean doPadding, jbyteArray output, jint outputOffset, jint outputSize) {
  aes_decryption_state *c_state;

  if( outputSize < 16 || state <= 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state or outputSize too small");
    return -1;
  }
  c_state = (aes_decryption_state *)state;
  if( c_state->direction != DECRYPTION ) {
    (*env)->ThrowNew(env, bad_arg, "Cannot finalize the passed state identifier");
    return -1;
  }

  // allow fetching of remaining bytes from cache
  if( !doPadding ) {
    (*env)->SetByteArrayRegion(env, output, outputOffset, c_state->cache_len, (jbyte *)c_state->cache);
    c_state->direction = FINALIZED;
    return c_state->cache_len;
  }

  // FIXME: is there anything else to do here?
  c_state->direction = FINALIZED;
  return 0;
}

#define MASTER_KEY_SIZE 32
JNIEXPORT jbyteArray JNICALL Java_com_keepassdroid_crypto_finalkey_NativeFinalKey_nTransformMasterKey(JNIEnv *env, jobject this, jbyteArray seed, jbyteArray key, jint rounds) {
  uint32_t i, flip = 0;
  jbyteArray result;
  aes_encrypt_ctx e_ctx[1] __attribute__ ((aligned (16)));
  sha256_ctx h_ctx[1] __attribute__ ((aligned (16)));
  uint8_t c_seed[MASTER_KEY_SIZE] __attribute__ ((aligned (16)));
  uint8_t key1[MASTER_KEY_SIZE] __attribute__ ((aligned (16)));
  uint8_t key2[MASTER_KEY_SIZE] __attribute__ ((aligned (16)));

  // step 1: housekeeping - sanity checks and fetch data from the JVM
  if( (*env)->GetArrayLength(env, seed) != MASTER_KEY_SIZE ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: the seed is not the correct size");
    return NULL;
  }
  if( (*env)->GetArrayLength(env, key) != MASTER_KEY_SIZE ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: the key is not the correct size");
    return NULL;
  }
  (*env)->GetByteArrayRegion(env, seed, 0, MASTER_KEY_SIZE, (jbyte *)c_seed);
  (*env)->GetByteArrayRegion(env, key, 0, MASTER_KEY_SIZE, (jbyte *)key1);

  // step 2: encrypt the hash "rounds" (default: 6000) times
  aes_encrypt_key(c_seed, MASTER_KEY_SIZE, e_ctx);
  for (i = 0; i < (uint32_t)rounds; i++) {
    if ( flip ) {
      aes_ecb_encrypt(key2, key1, MASTER_KEY_SIZE, e_ctx);
      flip = 0;
    } else {
      aes_ecb_encrypt(key1, key2, MASTER_KEY_SIZE, e_ctx);
      flip = 1;
    }
  }

  // step 3: final SHA256 hash
  sha256_begin(h_ctx);
  if( flip ) {
    sha256_hash(key2, MASTER_KEY_SIZE, h_ctx);
    sha256_end(key1, h_ctx);
    flip = 0;
  } else {
    sha256_hash(key1, MASTER_KEY_SIZE, h_ctx);
    sha256_end(key2, h_ctx);
    flip = 1;
  }

  // step 4: send the hash into the JVM
  result = (*env)->NewByteArray(env, MASTER_KEY_SIZE);
  if( flip )
    (*env)->SetByteArrayRegion(env, result, 0, MASTER_KEY_SIZE, (jbyte *)key2);
  else
    (*env)->SetByteArrayRegion(env, result, 0, MASTER_KEY_SIZE, (jbyte *)key1);
  return result;
}
#undef MASTER_KEY_SIZE

JNIEXPORT jint JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nGetEncryptCacheSize(JNIEnv* env, jobject this, jlong state) {
  aes_encryption_state *c_state;

  if( state <= 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state");
    return -1;
  }
  c_state = (aes_encryption_state *)state;
  if( c_state->direction != ENCRYPTION ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state");
    return -1;
  }
  return c_state->cache_len;
}

JNIEXPORT jint JNICALL Java_com_keepassdroid_crypto_NativeAESCipherSpi_nGetDecryptCacheSize(JNIEnv* env, jobject this, jlong state) {
  aes_decryption_state *c_state;

  if( state <= 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state");
    return -1;
  }
  c_state = (aes_decryption_state *)state;
  if( c_state->direction != DECRYPTION ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state");
    return -1;
  }
  return c_state->cache_len;
}
