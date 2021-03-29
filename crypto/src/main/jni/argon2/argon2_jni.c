/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>

#include "argon2.h"
#include "core.h"

static JavaVM *cached_vm;
static jclass bad_arg, io, no_mem;

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

    cls = (*env)->FindClass(env, "java/io/IOException");
    if( cls == NULL )
        return JNI_ERR;
    io = (*env)->NewGlobalRef(env, cls);
    if( io == NULL )
        return JNI_ERR;
    cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if( cls == NULL )
        return JNI_ERR;
    no_mem = (*env)->NewGlobalRef(env, cls);
    if( no_mem == NULL )
        return JNI_ERR;

    /*
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
    */

    return JNI_VERSION_1_6;
}

// called on garbage collection
JNIEXPORT void JNICALL JNI_OnUnload( JavaVM *vm, void *reserved ) {
JNIEnv *env;
    if((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6)) {
        return;
    }
    (*env)->DeleteGlobalRef(env, bad_arg);
    (*env)->DeleteGlobalRef(env, io);
    (*env)->DeleteGlobalRef(env, no_mem);

    /*
    (*env)->DeleteGlobalRef(env, bad_padding);
    (*env)->DeleteGlobalRef(env, short_buf);
    (*env)->DeleteGlobalRef(env, block_size);
    */

    return;
}



uint32_t getJNIArray(JNIEnv *env, jbyteArray array, uint8_t **output) {
    if (array == NULL) {
        *output = NULL;
        return 0;
    }

    uint32_t len = (*env)->GetArrayLength(env, array);
    uint8_t *buf = (uint8_t *)malloc(len);
    (*env)->GetByteArrayRegion(env, array, 0, len, (jbyte*) buf);

    *output = buf;

    return len;
}

void throwExceptionF(JNIEnv *env, jclass exception, const char *format, ...) {
    char message[512];

    va_list args;
    va_start(args, format);
    snprintf(message, 512, format, args);
    va_end(args);

    (*env)->ThrowNew(env, exception, message);
}

#define ARGON2_HASHLEN 32

JNIEXPORT jbyteArray
JNICALL Java_com_kunzisoft_encrypt_argon2_NativeArgon2KeyTransformer_nTransformKey(JNIEnv *env,
   jobject this, jint type, jbyteArray password, jbyteArray salt, jint parallelism, jint memory,
   jint iterations, jbyteArray secretKey, jbyteArray associatedData, jint version) {

    argon2_context context;
    uint8_t *out;

    out = (uint8_t *) malloc(ARGON2_HASHLEN);
    if (out == NULL) {
        throwExceptionF(env, no_mem, "Not enough memory for output hash array");
        return NULL;
    }

    uint8_t *passwordBuf;
    uint32_t passwordLen = getJNIArray(env, password, &passwordBuf);
    uint8_t *saltBuf;
    uint32_t saltLen = getJNIArray(env, salt, &saltBuf);
    uint8_t *secretBuf;
    uint32_t secretLen = getJNIArray(env, secretKey, &secretBuf);
    uint8_t *adBuf;
    uint32_t adLen = getJNIArray(env, associatedData, &adBuf);

    context.out = out;
    context.outlen = ARGON2_HASHLEN;
    context.pwd = passwordBuf;
    context.pwdlen = passwordLen;
    context.salt = saltBuf;
    context.saltlen = saltLen;
    context.secret = secretBuf;
    context.secretlen = secretLen;
    context.ad = adBuf;
    context.adlen = adLen;
    context.t_cost = (uint32_t) iterations;
    context.m_cost = (uint32_t) memory;
    context.lanes = (uint32_t) parallelism;
    context.threads = (uint32_t) parallelism;
    context.allocate_cbk = NULL;
    context.free_cbk = NULL;
    context.flags = ARGON2_DEFAULT_FLAGS;
    context.version = (uint32_t) version;

    int argonResult = argon2_ctx(&context, (argon2_type) type);

    jbyteArray result;
    if (argonResult != ARGON2_OK) {
        throwExceptionF(env, io, "Hash failed with code=%d", argonResult);
        result = NULL;
    } else {
        result = (*env)->NewByteArray(env, ARGON2_HASHLEN);
        (*env)->SetByteArrayRegion(env, result, 0, ARGON2_HASHLEN, (jbyte *) out);

    }

    clear_internal_memory(out, ARGON2_HASHLEN);
    free(out);
    if (passwordBuf != NULL) { free(passwordBuf); }
    if (saltBuf != NULL) { free(saltBuf); }
    if (secretBuf != NULL) { free(secretBuf); }
    if (adBuf != NULL) { free(adBuf); }

    return result;
}
