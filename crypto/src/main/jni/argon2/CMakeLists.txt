cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c89 -pthread")

include_directories(include/)
include_directories(src/)

add_library(
    argon2 SHARED
    src/argon2.c
    src/core.c
    src/encoding.c
    src/ref.c
    src/thread.c
    src/blake2/blake2b.c
    argon2_jni.c
)
