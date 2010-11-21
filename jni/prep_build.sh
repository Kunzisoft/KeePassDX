#!/bin/sh
http://gladman.plushost.co.uk/oldsite/AES/
AES_FILE="aes-src-29-04-09.zip"
SHA_FILE="sha2-07-01-07.zip"
EXTRACT_PATH=project/jni/

curl http://gladman.plushost.co.uk/oldsite/AES/$AES_FILE > $AES_FILE
unzip $AES_FILE -d $EXTRACT_PATH/aes
curl http://gladman.plushost.co.uk/oldsite/cryptography_technology/sha/$SHA_FILE > $SHA_FILE
unzip $SHA_FILE -d $EXTRACT_PATH/sha
