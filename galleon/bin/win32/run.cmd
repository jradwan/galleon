@echo off
REM
REM Run the Galleon server
REM
rem set JAVA_HOME=c:\Program Files\Java\jre1.4.2_06
set oldclasspath=%classpath%
set classpath=%classpath%;..\conf
set classpath=%classpath%;..\lib\galleon.jar
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
set classpath=%classpath%;..\lib\wrapper.jar
set classpath=%classpath%;..\lib\xbean.jar
set classpath=%classpath%;..\lib\xercesImpl.jar
set classpath=%classpath%;..\lib\xml-apis.jar
set classpath=%classpath%;..\lib\jampal.jar
set classpath=%classpath%;..\lib\concurrent.jar
set classpath=%classpath%;..\lib\bananas.jar
set classpath=%classpath%;..\lib\mp3dings.jar
set classpath=%classpath%;..\lib\yahoo_search.jar
set classpath=%classpath%;..\lib\jax.jar
set option=
if exist "%JAVA_HOME%/bin/server" set option=-server
java %option% -Xms64m -Xmx64m -Djava.awt.fonts="%JAVA_HOME%/lib/fonts" -Dawt.toolkit=com.eteks.awt.PJAToolkit org.lnicholls.galleon.server.Server
set classpath=%oldclasspath%
