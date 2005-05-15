@echo off
REM
REM Run the Galleon GUI
REM
rem set JAVA_HOME=c:\Program Files\Java\jre1.4.2_06
set oldclasspath=%classpath%
set classpath=..\build
set classpath=%classpath%;..\conf
set classpath=%classpath%;..\lib\galleon.jar
set classpath=%classpath%;..\lib\widgets.jar
set classpath=%classpath%;..\lib\commons.jar
set classpath=%classpath%;..\lib\derby.jar
set classpath=%classpath%;..\lib\dom4j.jar
set classpath=%classpath%;..\lib\hibernate.jar
set classpath=%classpath%;..\lib\hme.jar
set classpath=%classpath%;..\lib\jdai.jar
set classpath=%classpath%;..\lib\jdom.jar
set classpath=%classpath%;..\lib\javazoom.jar
set classpath=%classpath%;..\lib\js.jar
set classpath=%classpath%;..\lib\jshortcut.jar
set classpath=%classpath%;..\lib\log4j.jar
set classpath=%classpath%;..\lib\pja.jar
set classpath=%classpath%;..\lib\simulator.jar
set classpath=%classpath%;..\lib\wrapper.jar
set classpath=%classpath%;..\lib\xbean.jar
set classpath=%classpath%;..\lib\xercesImpl.jar
set classpath=%classpath%;..\lib\xml-apis.jar
set classpath=%classpath%;..\lib\jampal.jar
set classpath=%classpath%;..\lib\concurrent.jar
set classpath=%classpath%;..\lib\mp3dings.jar
set classpath=%classpath%;..\lib\bananas.jar
set classpath=%classpath%;..\lib\forms.jar
set classpath=%classpath%;..\lib\browserlauncher.jar
java -Xms32m -Xmx32m org.lnicholls.galleon.gui.Galleon
set classpath=%oldclasspath%
