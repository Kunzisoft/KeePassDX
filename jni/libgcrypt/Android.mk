
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libgcrypt

LOCAL_SRC_FILES :=  \
    src/visibility.c  \
    src/misc.c \
    src/global.c \
    src/sexp.c \
    src/hwfeatures.c \
    src/stdmem.c \
    src/secmem.c \
    src/missing-string.c \
    src/module.c \
    src/fips.c \
    src/ath.c  \
    src/hmac256.c \
    cipher/cipher.c \
    cipher/pubkey.c \
    cipher/ac.c \
    cipher/md.c \
    cipher/hmac-tests.c \
    cipher/primegen.c \
    cipher/hash-common.c \
    cipher/arcfour.c \
    cipher/blowfish.c \
    cipher/cast5.c \
    cipher/crc.c \
    cipher/des.c \
    cipher/dsa.c \
    cipher/elgamal.c \
    cipher/ecc.c \
    cipher/md4.c \
    cipher/md5.c \
    cipher/rijndael.c \
    cipher/rmd160.c \
    cipher/rsa.c \
    cipher/seed.c \
    cipher/serpent.c \
    cipher/sha1.c \
    cipher/sha256.c \
    cipher/sha512.c \
    cipher/tiger.c \
    cipher/whirlpool.c \
    cipher/twofish.c \
    cipher/rfc2268.c \
    mpi/mpi-add.c      \
    mpi/mpih-add1.c      \
    mpi/mpi-bit.c      \
    mpi/mpi-cmp.c      \
    mpi/mpi-div.c      \
    mpi/mpi-gcd.c      \
    mpi/mpi-inline.c   \
    mpi/mpi-inv.c      \
    mpi/mpi-mul.c      \
    mpi/mpih-mul1.c      \
    mpi/mpi-mod.c      \
    mpi/mpi-pow.c      \
    mpi/mpi-mpow.c     \
    mpi/mpi-scan.c     \
    mpi/mpih-sub1.c     \
    mpi/mpicoder.c     \
    mpi/mpiutil.c      \
    mpi/ec.c           \
    mpi/mpih-div.c           \
    random/random.c    \
    random/random-csprng.c    \
    random/random-fips.c      \
    random/rndhw.c            \
    random/rndlinux.c 
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libgpg-error/src/ $(LOCAL_PATH)/src $(LOCAL_PATH)/cipher $(LOCAL_PATH)/mpi $(LOCAL_PATH)/random

LOCAL_SHARED_LIBRARIES := libgpg-error

include $(BUILD_SHARED_LIBRARY)
