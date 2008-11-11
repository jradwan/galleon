#!/bin/bash -x
cd $(dirname $0)/../..
mydir=$(pwd)
cd $(dirname $0)
builddir=Galleon-build.pmdoc
srcdir=Galleon.pmdoc
for i in 01galleon-contents.xml 01galleon.xml index.xml; do
    sed -e s:${mydir}:/path/to/galleon:g $builddir/$i >$srcdir/$i
done
