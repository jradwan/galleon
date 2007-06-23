@echo off
REM
REM Run the Galleon server
REM
rem set JAVA_HOME=c:\Program Files\Java\jre1.5.0_11
set oldclasspath=%classpath%
set classpath=%classpath%;..\conf
set classpath=%classpath%;..\lib\galleon.jar
set classpath=%classpath%;..\lib\widgets.jar
set classpath=%classpath%;..\lib\commons.jar
set classpath=%classpath%;..\lib\cglib-2.1_3.jar
set classpath=%classpath%;..\lib\derby.jar
set classpath=%classpath%;..\lib\dom4j-1.6.1.jar
set classpath=%classpath%;..\lib\hibernate.jar
set classpath=%classpath%;..\lib\jdbc2_0-stdext.jar
set classpath=%classpath%;..\lib\hme-1.4.jar
set classpath=%classpath%;..\lib\hme-hd.jar
set classpath=%classpath%;..\lib\jdai-0.4.jar
set classpath=%classpath%;..\lib\jdom-1.0.jar
set classpath=%classpath%;..\lib\javazoom.jar
set classpath=%classpath%;..\lib\jl1.0.jar
set classpath=%classpath%;..\lib\js-1.6R5.jar
set classpath=%classpath%;..\lib\jshortcut-0.4.jar
set classpath=%classpath%;..\lib\log4j-1.2.14.jar
set classpath=%classpath%;..\lib\pja-2.5.jar
set classpath=%classpath%;..\lib\wrapper-3.2.3.jar
set classpath=%classpath%;..\lib\xbean-1.0.4.jar
set classpath=%classpath%;..\lib\xercesImpl-2.9.0.jar
set classpath=%classpath%;..\lib\xml-apis-2.9.0.jar
set classpath=%classpath%;..\lib\jampal-1.14.jar
set classpath=%classpath%;..\lib\concurrent.jar
set classpath=%classpath%;..\lib\bananas-1.3-custom.jar
set classpath=%classpath%;..\lib\jd3lib-a4.jar
set classpath=%classpath%;..\lib\yahoo_search-2.0.1.jar
set classpath=%classpath%;..\lib\jax.jar
set classpath=%classpath%;..\lib\informa-0.7.0.jar
set classpath=%classpath%;..\lib\activation.jar
set classpath=%classpath%;..\lib\mail-1.4.jar
set classpath=%classpath%;..\lib\htmlparser-1.5.jar
set classpath=%classpath%;..\lib\mediamanager-videoman-0.8.jar
set classpath=%classpath%;..\lib\jawin-1.0.19.jar
set classpath=%classpath%;..\lib\MHS-1.6.1-1697.jar
set classpath=%classpath%;..\lib\hme-host-sample-1.4.jar
set classpath=%classpath%;..\lib\upcoming.jar
set classpath=%classpath%;..\lib\smack.jar
set option=
if exist "%JAVA_HOME%/bin/server" set option=-server
java %option% -Xms64m -Xmx64m org.lnicholls.galleon.server.Server
set classpath=%oldclasspath%
