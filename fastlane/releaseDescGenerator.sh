#!/bin/bash
# Script to generate a description for the current release
# $1 versionCode
# $2 version Name
CHANGELOGPATH="./metadata/android/en-US/changelogs/$1.txt"
PACKAGEFREEPATH="../releases/KeePassDX-$2-free.apk"
PACKAGELIBREPATH="../releases/KeePassDX-$2-libre.apk"
RELEASEDESCPATH="../releases/KeePassDX-$2-desc"

echo "$(<$CHANGELOGPATH)" $'\n' >> $RELEASEDESCPATH
# Checksum Free
echo "Build Free" >> $RELEASEDESCPATH
echo "MD5 : $(md5sum $PACKAGEFREEPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "SHA1 : $(sha1sum $PACKAGEFREEPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "SHA256 : $(sha256sum $PACKAGEFREEPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "CRC32 : $(crc32 $PACKAGEFREEPATH)" >> $RELEASEDESCPATH
# Checksum Libre
echo $'\n'"Build Libre" >> $RELEASEDESCPATH
echo "MD5 : $(md5sum $PACKAGELIBREPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "SHA1 : $(sha1sum $PACKAGELIBREPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "SHA256 : $(sha256sum $PACKAGELIBREPATH  | cut -d ' ' -f 1)" >> $RELEASEDESCPATH
echo "CRC32 : $(crc32 $PACKAGELIBREPATH)" >> $RELEASEDESCPATH
