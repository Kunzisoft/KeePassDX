cmake_minimum_required(VERSION 3.4.1)

project("crypto")

# LTO: lets the compiler inline/optimize across translation units
# (e.g. blake2b.c <-> ref.c), portable win on every ABI (armeabi-v7a included).
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -flto=thin")

add_subdirectory(aes)
add_subdirectory(argon2)
