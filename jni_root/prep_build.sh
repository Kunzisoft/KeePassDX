#!/bin/sh

FILE="openssl-0.9.8l.tar.gz"
EXTRACT_PATH=project/jni/
PATCH=build.patch

ln -s ../ project
curl http://www.openssl.org/source/$FILE > $FILE
tar xzf $FILE -C $EXTRACT_PATH
patch -p1 -d $EXTRACT_PATH/openssl-0.9.8l < $PATCH
