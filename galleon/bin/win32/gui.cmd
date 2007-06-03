@echo off
REM
REM Run the Galleon GUI
REM
rem set JAVA_HOME=c:\Program Files\Java\jre1.5.0_11
set oldclasspath=%classpath%
set classpath=..\build
set classpath=%classpath%;..\conf
set classpath=%classpath%;..\lib\galleon.jar
set classpath=%classpath%;..\lib\widgets.jar
set classpath=%classpath%;..\lib\commons.jar
set classpath=%classpath%;..\lib\derby.jar
set classpath=%classpath%;..\lib\dom4j-1.6.1.jar
set classpath=%classpath%;..\lib\hibernate.jar
set classpath=%classpath%;..\lib\jdbc2_0-stdext.jar
set classpath=%classpath%;..\lib\hme-1.4.jar
set classpath=%classpath%;..\lib\jdai-0.4.jar
set classpath=%classpath%;..\lib\jdom-1.0.jar
set classpath=%classpath%;..\lib\javazoom.jar
set classpath=%classpath%;..\lib\jl1.0.jar
set classpath=%classpath%;..\lib\js-1.6R5.jar
set classpath=%classpath%;..\lib\jshortcut-0.4.jar
set classpath=%classpath%;..\lib\log4j-1.2.14.jar
set classpath=%classpath%;..\lib\pja-2.5.jar
set classpath=%classpath%;..\lib\simulator.jar
set classpath=%classpath%;..\lib\wrapper-3.2.3.jar
set classpath=%classpath%;..\lib\xbean-1.0.4.jar
set classpath=%classpath%;..\lib\xercesImpl-2.9.0.jar
set classpath=%classpath%;..\lib\xml-apis-2.9.0.jar
set classpath=%classpath%;..\lib\jampal-1.14.jar
set classpath=%classpath%;..\lib\concurrent.jar
set classpath=%classpath%;..\lib\jd3lib-a4.jar
set classpath=%classpath%;..\lib\bananas-1.3-custom.jar
set classpath=%classpath%;..\lib\forms.jar
set classpath=%classpath%;..\lib\browserlauncher.jar
set classpath=%classpath%;..\lib\hme-host-sample-1.4.jar
java -Xms32m -Xmx32m org.lnicholls.galleon.gui.Galleon %1
set classpath=%oldclasspath%
