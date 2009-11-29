#!/bin/sh
# Run FIPS CAVS tests
# Copyright 2008 Free Software Foundation, Inc.
#
# This file is free software; as a special exception the author gives
# unlimited permission to copy and/or distribute it, with or without
# modifications, as long as this notice is preserved.
#
# This file is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY, to the extent permitted by law; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# Instructions:
#
# 1. Cd to the libgcrypt/tests directory
#
# 2. Unpack the test vector tarball into subdirectory named "cavs". 
#    An example directory layout after unpacking might be:
#      libgcrypt/tests/cavs/AES/req/CBCGFSbox128.req
#      libgcrypt/tests/cavs/AES/req/CFB128MCT128.req
#
#    Note that below the "cavs" directory there should only be one
#    directory part named "req".  Further avoid directory part
#    names "resp".
#
# 3. Run this script from the libgcrypt/tests directory:
#      ./cavs_tests.sh
#
# 4. Send the result file cavs/CAVS_results-*.zip to the testing lab.
#

# Stop script if something unexpected happens.
set -e

# A global flag to keep track of errors.
errors_seen_file="$(pwd)/.#cavs_test.errors_seen.tmp"
[ -f "$errors_seen_file" ] && rm "$errors_seen_file"
continue_mode=no
[ "$1" = "--continue" ] && continue_mode=yes


# Function to run one test.  
# The argument is the request file name.
function run_one_test () {
    local reqfile="$1"
    local rspfile
    local tmprspfile
    local respdir
    local dflag=""

    tmprspfile=$(echo "$reqfile" | sed 's,.req$,.rsp,')
    rspfile=$(echo "$tmprspfile" | sed 's,/req/,/resp/,' )
    respdir=$(dirname "$rspfile")
    [ -f "$tmprspfile" ] && rm "$tmprspfile"
    [ -d "$respdir" ] || mkdir "$respdir"
    [ -f "$rspfile" ] &&  rm "$rspfile"

    if echo "$reqfile" | grep '/DSA/req/' >/dev/null 2>/dev/null; then
        dflag="-D"
    fi 
    
    if ./cavs_driver.pl -I libgcrypt $dflag "$reqfile"; then
      if [ -f "$tmprspfile" ]; then
          mv "$tmprspfile" "$rspfile"
      else 
          echo "failed test: $reqfile" >&2
          : >"$errors_seen_file"
      fi
    else
        echo "failed test: $reqfile rc=$?" >&2
        : >"$errors_seen_file"
    fi
}



# Save date and system architecure to construct the output archive name
DATE=$(date +%Y%m%d)
ARCH=$(arch || echo unknown)
result_file="CAVS_results-$ARCH-$DATE.zip"

for f in fipsdrv cavs_driver.pl; do
    if [ ! -f "./$f" ]; then
      echo "required program \"$f\" missing in current directory" >&2
      exit 2
    fi
done
if [ ! -d cavs ]; then
    echo "required directory \"cavs\" missing below current directory" >&2
    exit 2
fi
if [ ! zip -h >/dev/null 2>&1 ]; then
    echo "required program \"zip\" is not installed on this system" >&2
    exit 2
fi

# Set the PATH to this directory so that the perl script is able to
# find the test drivers.
PATH=.:$PATH

# Check whether there are any stale response files
find cavs -type f -name "*.rsp" | ( while read f ; do
    echo "Stale response file: $f" >&2
    any=yes
done
if [ "$any" = "yes" ]; then
    echo "Stale response files found" >&2
    if [ "$continue_mode" != "yes" ]; then
       echo "use option --continue if that is not a problem" >&2
       exit 1
    fi
fi
) || exit 1


# Find all test files and run the tests.
find cavs -type f -name "*.req" | while read f ; do
    echo "Running test file $f" >&2
    run_one_test "$f"
    if [ -f "$errors_seen_file" ]; then
        break;
    fi
done

if [ -f "$errors_seen_file" ]; then
    rm "$errors_seen_file"
    echo "Error encountered - not packing up response file" >&2
    exit 1
fi

echo "Packing up all response files" >&2
cd cavs
find . -type f -name "*rsp" -print | zip -@ "$result_file"

echo "Result file is: cavs/$result_file" >&2
