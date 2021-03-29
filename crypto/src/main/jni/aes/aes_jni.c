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
#include <pthread.h>
#include <jni.h>

/* Tune as desired */
#undef KPD_PROFILE
//#define KPD_DEBUG

#if defined(KPD_PROFILE)
#include <time.h>
#endif

#if defined(KPD_DEBUG)
#include <android/log.h>
#endif

#include "aes.h"
#include "sha2.h"

static JavaVM *cached_vm;
static jclass bad_arg, no_mem, bad_padding, short_buf, block_size;

typedef enum {
  ENCRYPTION,
  DECRYPTION,
  FINALIZED
} edir_t;

#define AES_BLOCK_SIZE 16
#define CACHE_SIZE 32

typedef struct _aes_state {
  edir_t direction;
  uint32_t cache_len;
  uint8_t iv[16], cache[CACHE_SIZE];
  uint8_t ctx[sizeof(aes_encrypt_ctx)]; // 244
} aes_state;

#define ENC_CTX(state) (((aes_encrypt_ctx *)((state)->ctx)))
#define DEC_CTX(state) (((aes_decrypt_ctx *)((state)->ctx)))
#define ALIGN_EXTRA 15
#define ALIGN16(x) (void *)(((uintptr_t)(x)+ALIGN_EXTRA) & ~ 0x0F)

JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *vm, void *reserved ) {
  JNIEnv *env;
  jclass cls;

  cached_vm = vm;
  if((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6))
    return JNI_ERR;

  cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if( cls == NULL )
    return JNI_ERR;
  bad_arg = (*env)->NewGlobalRef(env, cls);
  if( bad_arg == NULL )
    return JNI_ERR;

  cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
  if( cls == NULL )
    return JNI_ERR;
  no_mem = (*env)->NewGlobalRef(env, cls);
  if( no_mem == NULL )
    return JNI_ERR;

  cls = (*env)->FindClass(env, "javax/crypto/BadPaddingException");
  if( cls == NULL )
    return JNI_ERR;
  bad_padding = (*env)->NewGlobalRef(env, cls);

  cls = (*env)->FindClass(env, "javax/crypto/ShortBufferException");
  if( cls == NULL )
    return JNI_ERR;
  short_buf = (*env)->NewGlobalRef(env, cls);

  cls = (*env)->FindClass(env, "javax/crypto/IllegalBlockSizeException");
  if( cls == NULL )
    return JNI_ERR;
  block_size = (*env)->NewGlobalRef(env, cls);

  aes_init();

  return JNI_VERSION_1_6;
}

// called on garbage collection
JNIEXPORT void JNICALL JNI_OnUnload( JavaVM *vm, void *reserved ) {
  JNIEnv *env;
  if((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6)) {
    return;
  }
  (*env)->DeleteGlobalRef(env, bad_arg);
  (*env)->DeleteGlobalRef(env, no_mem);
  (*env)->DeleteGlobalRef(env, bad_padding);
  (*env)->DeleteGlobalRef(env, short_buf);
  (*env)->DeleteGlobalRef(env, block_size);
  return;
}

JNIEXPORT jlong JNICALL Java_com_kunzisoft_encrypt_aes_NativeAESCipherSpi_nInit(JNIEnv *env, jobject this, jboolean encrypting, jbyteArray key, jbyteArray iv) {
  uint8_t ckey[32];
  aes_state *state;
  jint key_len = (*env)->GetArrayLength(env, key);
  jint iv_len = (*env)->GetArrayLength(env, iv);

  if( ! ( key_len == 16 || key_len == 24 || key_len == 32 ) || iv_len != 16 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid length of key or iv");
    return -1;
  }

  state = (aes_state *)malloc(sizeof(aes_state));
  if( state == NULL ) {
    (*env)->ThrowNew(env, no_mem, "Cannot allocate memory for the encryption state");
    return -1;
  }
  memset(state, 0, sizeof(aes_state));

  (*env)->GetByteArrayRegion(env, key, (jint)0, key_len, (jbyte *)ckey);
  (*env)->GetByteArrayRegion(env, iv, (jint)0, iv_len, (jbyte *)state->iv);

  if( encrypting ) {
    state->direction = ENCRYPTION;
    aes_encrypt_key(ckey, key_len, ENC_CTX(state));
  } else {
    state->direction = DECRYPTION;
    aes_decrypt_key(ckey, key_len, DEC_CTX(state));
  }

  return (jlong)state;
}

JNIEXPORT void JNICALL Java_com_kunzisoft_encrypt_aes_NativeAESCipherSpi_nCleanup(JNIEnv *env, jclass this, jlong state) {
  free((void *)state);
}

/*
  TODO:
  It seems like the android implementation of the AES cipher stays a
  block behind with update calls. So, if you do an update for 16 bytes,
  it will return nothing in the output buffer.  Then, it is the finalize
  call that will return the last block stripping off padding if it is
  not a full block.
*/

JNIEXPORT jint JNICALL Java_com_kunzisoft_encrypt_aes_NativeAESCipherSpi_nUpdate(JNIEnv *env, jobject this,
	jlong state, jbyteArray input, jint inputOffset, jint inputLen, jbyteArray output, jint outputOffset, jint outputSize) {
  int aes_ret;
  uint32_t outLen, bytes2cache, cryptLen;
  void *in, *out;
  uint8_t *c_input, *c_output;
  aes_state *c_state;

  #if defined(KPD_DEBUG)
  __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nUpdate", "entry: inputLen=%d, outputSize=%d", inputLen, outputSize);
  #endif

  // step 1: first, some housecleaning
  if( !inputLen || !outputSize || outputOffset < 0 || !input || !output ) {
    (*env)->ThrowNew(env, bad_arg, "nUpdate: called with 1 or more invalid arguments");
    return -1;
  }
  c_state = (aes_state *)state;
  if( c_state->direction == FINALIZED ) {
    (*env)->ThrowNew(env, bad_arg, "Trying to update a finalized state");
    return -1;
  }

  // step 1.5: calculate cryptLen and outLen
  cryptLen = inputLen + c_state->cache_len;
  if( cryptLen < CACHE_SIZE ) {
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)(c_state->cache + c_state->cache_len));
    c_state->cache_len = cryptLen;
    return 0;
  }
  // now we're guaranteed that cryptLen >= CACHE_SIZE (32)
  bytes2cache = (cryptLen & 15) + AES_BLOCK_SIZE; // mask bottom 4 bits plus 1 block
  outLen = (cryptLen - bytes2cache); // output length is now aligned to a 16-byte boundary
  if( outLen > (uint32_t)outputSize ) {
    (*env)->ThrowNew(env, bad_arg, "Output buffer does not have enough space");
    return -1;
  }

  // step 2: allocate memory to hold input and output data
  in = malloc(cryptLen+ALIGN_EXTRA);
  if( in == NULL ) {
    (*env)->ThrowNew(env, no_mem, "Unable to allocate heap space for encryption input");
    return -1;
  }
  c_input = ALIGN16(in);

  out = malloc(outLen+ALIGN_EXTRA);
  if( out == NULL ) {
    free(in);
    (*env)->ThrowNew(env, no_mem, "Unable to allocate heap space for encryption output");
    return -1;
  }
  c_output = ALIGN16(out);

  // step 3: copy data from Java and en/decrypt it
  if( c_state->cache_len ) {
    memcpy(c_input, c_state->cache, c_state->cache_len);
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)(c_input + c_state->cache_len));
  } else {
    (*env)->GetByteArrayRegion(env, input, inputOffset, inputLen, (jbyte *)c_input);
  }
  if( c_state->direction == ENCRYPTION )
    aes_ret = aes_cbc_encrypt(c_input, c_output, outLen, c_state->iv, ENC_CTX(c_state));
  else
    aes_ret = aes_cbc_decrypt(c_input, c_output, outLen, c_state->iv, DEC_CTX(c_state));
  if( aes_ret != EXIT_SUCCESS ) {
    free(in);
    free(out);
    (*env)->ThrowNew(env, bad_arg, "Failed to encrypt input data"); // FIXME: get a better exception class for this...
    return -1;
  }
  (*env)->SetByteArrayRegion(env, output, outputOffset, outLen, (jbyte *)c_output);

  // step 4: cleanup and return
  if( bytes2cache ) {
    c_state->cache_len = bytes2cache; // set new cache length
    memcpy(c_state->cache, (c_input + outLen), bytes2cache); // cache overflow bytes for next call
  } else {
    c_state->cache_len = 0;
  }

  free(in);
  free(out);

  #if defined(KPD_DEBUG)
  __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nUpdate", "exit: outLen=%d", outLen);
  #endif

  return outLen;
}

/*
  outputSize must be at least 32 for encryption since the buffer may contain >= 1 full block
  outputSize must be at least 16 for decryption
*/
JNIEXPORT jint JNICALL Java_com_kunzisoft_encrypt_aes_NativeAESCipherSpi_nFinal(JNIEnv *env, jobject this,
	jlong state, jboolean doPadding, jbyteArray output, jint outputOffset, jint outputSize) {
  int i;
  uint32_t padValue, paddedCacheLen;
  uint8_t final_output[CACHE_SIZE] __attribute__ ((aligned (16)));
  aes_state *c_state;

  #if defined(KPD_DEBUG)
  __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nFinal", "entry: outputOffset=%d, outputSize=%d", outputOffset, outputSize);
  #endif

  if( !output || outputOffset < 0 ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid argument(s) passed to nFinal");
    return -1;
  }
  c_state = (aes_state *)state;
  if( c_state->direction == FINALIZED ) {
    (*env)->ThrowNew(env, bad_arg, "This state has already been finalized");
    return -1;
  }

  // allow fetching of remaining bytes from cache
  if( !doPadding ) {
    (*env)->SetByteArrayRegion(env, output, outputOffset, c_state->cache_len, (jbyte *)c_state->cache);
    c_state->direction = FINALIZED;
    return c_state->cache_len;
  }

  #if defined(KPD_DEBUG)
  __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nFinal", "crypto operation starts");
  #endif

  if( c_state->direction == ENCRYPTION ) {
    if( c_state->cache_len >= 16 ) {
      paddedCacheLen = 32;
    } else {
      paddedCacheLen = 16;
    }
    if( outputSize < (jint)paddedCacheLen ) {
      (*env)->ThrowNew(env, short_buf, "Insufficient space in output buffer");
      return -1;
    }
    padValue = paddedCacheLen - c_state->cache_len;
    if(!padValue) padValue = 16;
    memset(c_state->cache + c_state->cache_len, padValue, padValue);
    if( aes_cbc_encrypt(c_state->cache, final_output, paddedCacheLen, c_state->iv, ENC_CTX(c_state)) != EXIT_SUCCESS ) {
      (*env)->ThrowNew(env, bad_arg, "Failed to encrypt the final data block(s)"); // FIXME: get a better exception class for this...
      return -1;
    }
    (*env)->SetByteArrayRegion(env, output, outputOffset, paddedCacheLen, (jbyte *)final_output);
    c_state->direction = FINALIZED;
    #if defined(KPD_DEBUG)
    __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nFinal", "encryption operation completed, returning %d bytes", paddedCacheLen);
    #endif
    return paddedCacheLen;
  } else { // DECRYPTION

    paddedCacheLen = c_state->cache_len;
    if( outputSize < (jint)paddedCacheLen ) {
      (*env)->ThrowNew(env, short_buf, "Insufficient space in output buffer");
      return -1;
    }
    if( paddedCacheLen != AES_BLOCK_SIZE ) {
      (*env)->ThrowNew(env, bad_padding, "Incomplete final block in cache for decryption state");
      return -1;
    }
    if( aes_cbc_decrypt(c_state->cache, final_output, paddedCacheLen, c_state->iv, DEC_CTX(c_state)) != EXIT_SUCCESS ) {
      (*env)->ThrowNew(env, bad_arg, "Failed to decrypt the final data block(s)"); // FIXME: get a better exception class for this...
      return -1;
    }
    padValue = final_output[paddedCacheLen-1];

    int badPadding;
    badPadding = padValue > AES_BLOCK_SIZE;

    if (!badPadding) {
        for(i = paddedCacheLen-1; final_output[i] == padValue && i >= 0; i--) {
            if (final_output[i] != padValue) {
                badPadding = 1;
                break;
            }
        }
    }

    #if defined(KPD_DEBUG)
    __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nFinal", "padValue=%d", padValue);
    #endif
    if( badPadding ) {
      (*env)->ThrowNew(env, bad_padding, "Failed to verify padding during decryption");
      return -1;
    }

    int outputSize = AES_BLOCK_SIZE - padValue;

    (*env)->SetByteArrayRegion(env, output, outputOffset, outputSize, (jbyte *)final_output);
    c_state->direction = FINALIZED;
    #if defined(KPD_DEBUG)
    __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nFinal", "decryption operation completed, returning %d bytes", outputSize);
    #endif
    return outputSize;
  }
}

JNIEXPORT jint JNICALL Java_com_kunzisoft_encrypt_aes_NativeAESCipherSpi_nGetCacheSize(JNIEnv* env, jobject this, jlong state) {
  aes_state *c_state;

  c_state = (aes_state *)state;
  if( c_state->direction == FINALIZED ) {
    (*env)->ThrowNew(env, bad_arg, "Invalid state");
    return -1;
  }
  return c_state->cache_len;
}

#define MASTER_KEY_SIZE 32

typedef struct _master_key {
  uint64_t rounds;
  uint32_t done[2];
  pthread_mutex_t lock1, lock2; // these lock the two halves of the key material
  uint8_t c_seed[MASTER_KEY_SIZE] __attribute__ ((aligned (16)));
  uint8_t key1[MASTER_KEY_SIZE] __attribute__ ((aligned (16)));
  uint8_t key2[MASTER_KEY_SIZE] __attribute__ ((aligned (16)));
} master_key;


uint32_t generate_key_material(void *arg) {
  #if defined(KPD_PROFILE)
  struct timespec start, end;
  #endif
  uint32_t i, flip = 0;
  uint8_t *key1, *key2;
  master_key *mk = (master_key *)arg;
  aes_encrypt_ctx e_ctx[1] __attribute__ ((aligned (16)));

  if( mk->done[0] == 0 && pthread_mutex_trylock(&mk->lock1) == 0 ) {
    key1 = mk->key1;
    key2 = mk->key2;
  } else if( mk->done[1] == 0 && pthread_mutex_trylock(&mk->lock2) == 0 ) {
    key1 = mk->key1 + (MASTER_KEY_SIZE/2);
    key2 = mk->key2 + (MASTER_KEY_SIZE/2);
  } else {
    // this can only be scaled to two threads
    pthread_exit( (void *)(-1) );
  }

  #if defined(KPD_PROFILE)
  clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start);
  #endif

  aes_encrypt_key256(mk->c_seed, e_ctx);
  for (i = 0; i < mk->rounds; i++) {
    if ( flip ) {
      aes_encrypt(key2, key1, e_ctx);
      flip = 0;
    } else {
      aes_encrypt(key1, key2, e_ctx);
      flip = 1;
    }
  }

  #if defined(KPD_PROFILE)
  clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end);
  if( key1 == mk->key1 )
    __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nTransformMasterKey", "Thread 1 master key transformation took ~%d seconds", (end.tv_sec-start.tv_sec));
  else
    __android_log_print(ANDROID_LOG_INFO, "aes_jni.c/nTransformMasterKey", "Thread 2 master key transformation took ~%d seconds", (end.tv_sec-start.tv_sec));
  #endif

  if( key1 == mk->key1 ) {
    mk->done[0] = 1;
    pthread_mutex_unlock(&mk->lock1);
  } else {
    mk->done[1] = 1;
    pthread_mutex_unlock(&mk->lock2);
  }

  return flip;
}

JNIEXPORT jbyteArray JNICALL Java_com_kunzisoft_encrypt_aes_NativeAESKeyTransformer_nTransformKey(JNIEnv *env, jobject this, jbyteArray seed, jbyteArray key, jlong rounds) {
  master_key mk;
  uint32_t flip;
  pthread_t t1, t2;
  int iret;
  void *vret1, *vret2;
  jbyteArray result;
  sha256_ctx h_ctx[1] __attribute__ ((aligned (16)));

  // step 1: housekeeping - sanity checks and fetch data from the JVM
  if( (*env)->GetArrayLength(env, seed) != MASTER_KEY_SIZE ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: the seed is not the correct size");
    return NULL;
  }
  if( (*env)->GetArrayLength(env, key) != MASTER_KEY_SIZE ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: the key is not the correct size");
    return NULL;
  }
  if( rounds < 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: illegal number of encryption rounds");
    return NULL;
  }
  mk.rounds = (uint64_t)rounds;
  mk.done[0] = mk.done[1] = 0;
  if( pthread_mutex_init(&mk.lock1, NULL) != 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: failed to initialize the mutex for thread 1"); // FIXME: get a better exception class for this...
    return NULL;
  }
  if( pthread_mutex_init(&mk.lock2, NULL) != 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: failed to initialize the mutex for thread 2"); // FIXME: get a better exception class for this...
    return NULL;
  }
  (*env)->GetByteArrayRegion(env, seed, 0, MASTER_KEY_SIZE, (jbyte *)mk.c_seed);
  (*env)->GetByteArrayRegion(env, key, 0, MASTER_KEY_SIZE, (jbyte *)mk.key1);

  // step 2: encrypt the hash "rounds"
  iret = pthread_create( &t1, NULL, (void*)generate_key_material, (void*)&mk );
  if( iret != 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: failed to launch thread 1"); // FIXME: get a better exception class for this...
    return NULL;
  }
  iret = pthread_create( &t2, NULL, (void*)generate_key_material, (void*)&mk );
  if( iret != 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: failed to launch thread 2"); // FIXME: get a better exception class for this...
    return NULL;
  }
  iret = pthread_join( t1, &vret1 );
  if( iret != 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: failed to join thread 1"); // FIXME: get a better exception class for this...
    return NULL;
  }
  iret = pthread_join( t2, &vret2 );
  if( iret != 0 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: failed to join thread 2"); // FIXME: get a better exception class for this...
    return NULL;
  }
  if( vret1 == (void *)(-1) || vret2 == (void *)(-1) || vret1 != vret2 ) {
    (*env)->ThrowNew(env, bad_arg, "TransformMasterKey: invalid flip value(s) from completed thread(s)"); // FIXME: get a better exception class for this...
    return NULL;
  } else {
    flip = (uint32_t)vret1;
  }

  // step 3: final SHA256 hash
  sha256_begin(h_ctx);
  if( flip ) {
    sha256_hash(mk.key2, MASTER_KEY_SIZE, h_ctx);
    sha256_end(mk.key1, h_ctx);
    flip = 0;
  } else {
    sha256_hash(mk.key1, MASTER_KEY_SIZE, h_ctx);
    sha256_end(mk.key2, h_ctx);
    flip = 1;
  }

  // step 4: send the hash into the JVM
  result = (*env)->NewByteArray(env, MASTER_KEY_SIZE);
  if( flip )
    (*env)->SetByteArrayRegion(env, result, 0, MASTER_KEY_SIZE, (jbyte *)mk.key2);
  else
    (*env)->SetByteArrayRegion(env, result, 0, MASTER_KEY_SIZE, (jbyte *)mk.key1);

  return result;
}
#undef MASTER_KEY_SIZE

