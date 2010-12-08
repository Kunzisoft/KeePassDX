#!/bin/sh
AES_FILE="aes-src-29-04-09.zip"
SHA_FILE="sha2-07-01-07.zip"

curl http://gladman.plushost.co.uk/oldsite/AES/$AES_FILE > $AES_FILE
unzip $AES_FILE -d aes
curl http://gladman.plushost.co.uk/oldsite/cryptography_technology/sha/$SHA_FILE > $SHA_FILE
unzip $SHA_FILE -d sha
