#!/bin/sh -x
basedir=`dirname $0`/../..
cd $basedir
if [ $# -gt 0 ]; then
    rm -rf build.unix
    ant -Dplatform=unix package
    mv build build.unix
fi
rm -f galleon-2.5.3-linux.zip
(cd build.unix && zip -r ../galleon-2.5.3-linux.zip *)

