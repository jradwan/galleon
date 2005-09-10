@echo off
REM
REM Run the Galleon server
REM
rem set JAVA_HOME=c:\Program Files\Java\jre1.4.2_06
set oldclasspath=%classpath%
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
set classpath=%classpath%;..\lib\informa.jar
set classpath=%classpath%;..\lib\activation.jar
set classpath=%classpath%;..\lib\mail.jar
set classpath=%classpath%;..\lib\htmlparser.jar
set classpath=%classpath%;..\lib\mediamanager.jar
set classpath=%classpath%;..\lib\jawin.jar
set classpath=%classpath%;..\lib\MHS.jar
set classpath=%classpath%;..\lib\hme-host-sample.jar
set option=
if exist "%JAVA_HOME%/bin/server" set option=-server
java %option% -Xms64m -Xmx64m org.lnicholls.galleon.server.Server
set classpath=%oldclasspath%
