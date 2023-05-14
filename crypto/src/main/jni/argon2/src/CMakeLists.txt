cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUSE_SHA256")

include_directories(aes/)
include_directories(sha/)

add_library(
    aes SHARED
    aes_jni.c
    aes/aescrypt.c
    aes/aeskey.c
    aes/aes_modes.c
    aes/aestab.c
    sha/hmac.c
    sha/sha1.c
    sha/sha2.c
)

find_library(log-lib log)

target_link_libraries(aes ${log-lib})
