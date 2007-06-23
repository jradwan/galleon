#!/bin/bash
#
# Run the Galleon server
#
# You may have to set JAVA_HOME to the correct value for your system
#JAVA_HOME=/usr/java/jre
mydir=`dirname "$0"`
cd "$mydir"
CLASSPATH=../conf
for j in ../lib/*.jar 
do 
CLASSPATH=$CLASSPATH:$j 
done 

OPTION=""
if [ -f "$JAVA_HOME/bin/server" ]; then
OPTION=-server
fi
mkdir -p ../data/tmp
java $OPTION -cp $CLASSPATH -Xms64m -Xmx64m -Djava.awt.fonts="$JAVA_HOME/lib/fonts" -Dawt.toolkit=com.eteks.awt.PJAToolkit -Djava.net.preferIPv4Stack=true -Djava.io.tmpdir="`pwd`/../data/tmp" org.lnicholls.galleon.server.Server

