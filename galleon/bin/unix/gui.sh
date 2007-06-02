#!/bin/bash
#
# Run the Galleon GUI
#
# You may have to set JAVA_HOME to the correct value for your system
#JAVA_HOME=/usr/java/jre
mydir=`dirname $0`
cd $mydir
CLASSPATH=../conf
for j in ../lib/*.jar 
do 
CLASSPATH=$CLASSPATH:$j 
done

java -cp $CLASSPATH -Xms32m -Xmx32m org.lnicholls.galleon.gui.Galleon $1
