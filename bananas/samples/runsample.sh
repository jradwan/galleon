#!/bin/sh

#
# This script will run the bananas sample.

# To use the script:
#
# 1. Java must be in your path, or you must modify this script to use
#    your installed java.
#
# 2. You must set HME_HOME to point at your HME SDK directory.
#

#HME_HOME=
if [ -z "$HME_HOME" ] ; then
    echo You must set HME_HOME.
    exit 1
fi

java -cp $HME_HOME/hme.jar:$HME_HOME/hme-host-sample.jar:../bananas.jar:samples.jar com.tivo.hme.host.sample.Main com.tivo.hme.samples.bananas.BananasSample
