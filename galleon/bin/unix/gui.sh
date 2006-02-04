#!/bin/bash
#
# Run the Galleon server
#
# You may have to set JAVA_HOME to the correct value for your system
#JAVA_HOME=/usr/java/jre
OLDCLASSPATH=$CLASSPATH
CLASSPATH=../conf
for j in ../lib/*.jar 
do 
CLASSPATH=$CLASSPATH:$j 
done

java -cp $CLASSPATH -Xms32m -Xmx32m org.lnicholls.galleon.gui.Galleon $1
CLASSPATH=$OLDCLASSPATH

