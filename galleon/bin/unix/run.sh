#!/bin/bash
#
# Run the Galleon server
#
# You may have to set JAVA_HOME to the correct value for your system
JAVA_HOME=/usr/java/jre
OLDCLASSPATH=$CLASSPATH
CLASSPATH=$CLASSPATH:../conf
for j in ../lib/*.jar 
do 
CLASSPATH=$CLASSPATH:$j 
done 

OPTION=""
if [ -f "$JAVA_HOME/bin/server" ]; then
OPTION=-server
fi
java $OPTION -cp $CLASSPATH -Xms64m -Xmx64m -Djava.awt.fonts="$JAVA_HOME/lib/fonts" -Dawt.toolkit=com.eteks.awt.PJAToolkit org.lnicholls.galleon.server.Server
CLASSPATH=$OLDCLASSPATH
