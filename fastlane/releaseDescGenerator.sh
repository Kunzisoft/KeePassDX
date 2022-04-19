#!/bin/bash
# Script to generate a description for the current release
# $1 versionCode
# $2 version Name
CHANGELOGPATH="./metadata/android/en-US/changelogs/$1.txt"
PACKAGEPATH="../releases/KeePassDX-$2.apk"
RELEASEDESCPATH="../releases/KeePassDX-$2_desc"

echo "$(<$CHANGELOGPATH)" $'\n' >> $RELEASEDESCPATH
# Checksum
echo "MD5 : $(md5sum $PACKAGEPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "SHA1 : $(sha1sum $PACKAGEPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "SHA256 : $(sha256sum $PACKAGEPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "CRC32 : $(crc32 $PACKAGEPATH)" >> $RELEASEDESCPATH
