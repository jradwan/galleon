; Instructions
; ------------
; This script is used by the Nullsoft Scriptable Install System (NSIS) which is an open source project at: http://nsis.sourceforge.net
; It is released under an open source license and is completely free for any use.

; To compile the script download and install the NSIS 2.1: http://nsis.sourceforge.net
; Download the Service Control Manager plugin: http://nsis.sourceforge.net/archive/viewpage.php?pageid=385
; This plugin allows services to be controlled on NT/2K/XP. Copy the nsSCM.dll file into C:\Program Files\NSIS\Plugins
; Download the JRE installer and place it in the paths indicated below.

; To edit the script download the HM NIS EDIT at: http://hmne.sourceforge.net
; It is a Free NSIS Editor/IDE for NSIS.

; Script generated by the HM NIS Edit Script Wizard: http://hmne.sourceforge.net
; HM NIS Edit Wizard helper defines
!define PRODUCT_NAME "Galleon"
!define PRODUCT_VERSION "1.2.0"
!define PRODUCT_PUBLISHER "Galleon"
!define PRODUCT_WEB_SITE "http://galleon.sourceforge.net"
!define PRODUCT_CONFIGURE '"$SYSDIR\javaw.exe" -classpath ..\conf\;galleon.jar;log4j.jar;forms.jar;commons.jar;concurrent.jar;hibernate.jar;hme.jar;pja.jar;dom4j.jar org.lnicholls.galleon.gui.Galleon'
!define PRODUCT_BUILD_DIR "d:\galleon\build"

!define JRE_VERSION "1.5.0"
!define JRE_URL "d:\download\jre-1_5_0_04-windows-i586-p.exe"
!define JRE_PATH $R0
!define TEMP $R1
!define TEMP2 $R2
!define TEMP3 $R3
!define TEMP4 $R4
!define VAL1 $R5
!define VAL2 $R6
!define STATUS $R7

!define DOWNLOAD_JRE_FLAG $8

; MUI 1.67 compatible ------
!include "MUI.nsh"
!include "UpgradeDLL.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\win-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\win-uninstall.ico"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Show documentation."
!define MUI_FINISHPAGE_RUN_TEXT "Launch configurator."

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "Setup.exe"
InstallDir "$PROGRAMFILES\${PRODUCT_NAME}"
ShowInstDetails show
ShowUnInstDetails show

; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!insertmacro MUI_PAGE_LICENSE "${PRODUCT_BUILD_DIR}\copying"

; Determine if the JRE is already installed
Page custom CheckInstalledJRE

; Components page
!insertmacro MUI_PAGE_COMPONENTS
; Directory page
!insertmacro MUI_PAGE_DIRECTORY

; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!define MUI_FINISHPAGE_SHOWREADME "${PRODUCT_WEB_SITE}"
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchLink"

Function LaunchLink
  ExecShell "" "$SMPROGRAMS\${PRODUCT_NAME}\Configure.lnk"
FunctionEnd

!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; MUI end ------

; Stop the Galleon service
Section -StopGalleon SEC00
  ;http://nsis.sourceforge.net/archive/viewpage.php?pageid=385
  ; Is service already installed?
  nsSCM::QueryStatus /NOUNLOAD "${PRODUCT_NAME}"
  Pop ${STATUS} ; return error/success
  Pop $1 ; return service status
  StrCmp ${STATUS} "error" End
  DetailPrint "Found ${PRODUCT_NAME} service"
  StrCmp $1 "4" 0 End
  DetailPrint "Stopping ${PRODUCT_NAME} service"
  ; Attempt to stop service
  nsSCM::Stop /NOUNLOAD "${PRODUCT_NAME}"
  Pop $2 ; return error/success
  StrCmp $2 "success" Wait
  MessageBox MB_ICONEXCLAMATION|MB_OK "The ${PRODUCT_NAME} service could not be stopped"
  Quit

Wait:
  DetailPrint "Ending ${PRODUCT_NAME} service"
  Sleep 10000 ; wait for JVM to stop and release resources

End:

SectionEnd

; Install the JRE if not already installed
Section "JRE" jre
  StrCmp ${DOWNLOAD_JRE_FLAG} "NoDownload" End

  DetailPrint "Starting the JRE installation"
  File /oname=$TEMP\jre_setup.exe ${JRE_URL}
  DetailPrint "Launching JRE setup"
  ExecWait "$TEMP\jre_setup.exe" $0
  BringToFront
  DetailPrint "Setup finished"
  Delete "$TEMP\jre_setup.exe"
  StrCmp $0 "0" InstallVerif 0
  Push "The JRE setup has been abnormally interrupted."
  Goto ExitInstallJRE

InstallVerif:
  DetailPrint "Checking the JRE Setup's outcome"
  Call DetectJRE
  Pop $0
  StrCmp $0 "OK" JavaExeVerif 0
  Push "The JRE setup failed"
  Goto ExitInstallJRE

JavaExeVerif:
  Pop $1
  IfFileExists $1 JREPathStorage 0
  Push "The following file : $1, cannot be found."
  Goto ExitInstallJRE

JREPathStorage:
  DetailPrint "The JRE was found at: $1"
  Goto JavaHome

ExitInstallJRE:
  Pop $2
  MessageBox MB_OK "The setup is about to be interrupted for the following reason : $2"
  Quit
  
JavaHome:
  Push "JAVA_HOME"
  Push $1
  Call WriteEnvStr

End:

SectionEnd

; Install the files for Galleon
Section -installGalleon SEC01
  DetailPrint "Installing Galleon executables"
  SetOverwrite ifnewer
  SetOutPath "$INSTDIR\bin"

  File ${PRODUCT_BUILD_DIR}\bin\install.cmd
  File ${PRODUCT_BUILD_DIR}\bin\run.cmd
  File ${PRODUCT_BUILD_DIR}\bin\uninstall.cmd
  File ${PRODUCT_BUILD_DIR}\bin\Wrapper.exe
  File ${PRODUCT_BUILD_DIR}\bin\gui.cmd

  DetailPrint "Installing Galleon configuration files"
  SetOutPath "$INSTDIR\conf"
  ; Keep the previous configuration file
  IfFileExists "$INSTDIR\conf\configure.xml" 0 InstallConfiguration
  CopyFiles /SILENT "$INSTDIR\conf\configure.xml" "$INSTDIR\conf\configure.xml.rpmsave"
  Goto CopyFiles

InstallConfiguration:
  File ${PRODUCT_BUILD_DIR}\conf\configure.xml

CopyFiles:
  File ${PRODUCT_BUILD_DIR}\conf\log4j.xml
  File ${PRODUCT_BUILD_DIR}\conf\wrapper.conf
  File ${PRODUCT_BUILD_DIR}\conf\derby.properties
  File ${PRODUCT_BUILD_DIR}\conf\ehcache.xml
  File ${PRODUCT_BUILD_DIR}\conf\hibernate.properties

  SetOutPath "$INSTDIR\media\images"
  File ${PRODUCT_BUILD_DIR}\media\images\cross.jpg
  File ${PRODUCT_BUILD_DIR}\media\images\galleon.ico
  SetOutPath "$INSTDIR\media\winamp"
  File ${PRODUCT_BUILD_DIR}\media\winamp\metrix.wsz
  File ${PRODUCT_BUILD_DIR}\media\winamp\metrix_metal-dream.wsz
  File ${PRODUCT_BUILD_DIR}\media\winamp\metrix_metal_dream_gold.wsz
  File ${PRODUCT_BUILD_DIR}\media\winamp\metrix_metal_dream_green.wsz
  File ${PRODUCT_BUILD_DIR}\media\winamp\metrix_metal_dream_red.wsz

  DetailPrint "Installing Galleon libraries"
  SetOutPath "$INSTDIR\lib"
  File /oname=activation.jar ${PRODUCT_BUILD_DIR}\lib\activation.jar
  File /oname=bananas.jar ${PRODUCT_BUILD_DIR}\lib\bananas.jar
  File /oname=browserlauncher.jar ${PRODUCT_BUILD_DIR}\lib\browserlauncher.jar
  File /oname=commons.jar ${PRODUCT_BUILD_DIR}\lib\commons.jar
  File /oname=concurrent.jar ${PRODUCT_BUILD_DIR}\lib\concurrent.jar
  File /oname=derby.jar ${PRODUCT_BUILD_DIR}\lib\derby.jar
  File /oname=dom4j.jar ${PRODUCT_BUILD_DIR}\lib\dom4j.jar
  File /oname=forms.jar ${PRODUCT_BUILD_DIR}\lib\forms.jar
  File /oname=galleon.jar ${PRODUCT_BUILD_DIR}\lib\galleon.jar
  File /oname=hibernate.jar ${PRODUCT_BUILD_DIR}\lib\hibernate.jar
  File /oname=hme.jar ${PRODUCT_BUILD_DIR}\lib\hme.jar
  File /oname=informa.jar ${PRODUCT_BUILD_DIR}\lib\informa.jar
  File /oname=jampal.jar ${PRODUCT_BUILD_DIR}\lib\jampal.jar
  File /oname=javazoom.jar ${PRODUCT_BUILD_DIR}\lib\javazoom.jar
  File /oname=jax.jar ${PRODUCT_BUILD_DIR}\lib\jax.jar
  File /oname=jdai.jar ${PRODUCT_BUILD_DIR}\lib\jdai.jar
  File /oname=jdom.jar ${PRODUCT_BUILD_DIR}\lib\jdom.jar
  File /oname=js.jar ${PRODUCT_BUILD_DIR}\lib\js.jar
  File /oname=jshortcut.dll ${PRODUCT_BUILD_DIR}\lib\jshortcut.dll
  File /oname=jshortcut.jar ${PRODUCT_BUILD_DIR}\lib\jshortcut.jar
  File /oname=log4j.jar ${PRODUCT_BUILD_DIR}\lib\log4j.jar
  File /oname=mail.jar ${PRODUCT_BUILD_DIR}\lib\mail.jar
  File /oname=mp3dings.jar ${PRODUCT_BUILD_DIR}\lib\mp3dings.jar
  File /oname=pja.jar ${PRODUCT_BUILD_DIR}\lib\pja.jar
  File /oname=widgets.jar ${PRODUCT_BUILD_DIR}\lib\widgets.jar
  File /oname=Wrapper.dll ${PRODUCT_BUILD_DIR}\lib\wrapper.dll
  File /oname=wrapper.jar ${PRODUCT_BUILD_DIR}\lib\wrapper.jar
  File /oname=xbean.jar ${PRODUCT_BUILD_DIR}\lib\xbean.jar
  File /oname=xercesImpl.jar ${PRODUCT_BUILD_DIR}\lib\xercesImpl.jar
  File /oname=xml-apis.jar ${PRODUCT_BUILD_DIR}\lib\xml-apis.jar
  File /oname=yahoo_search.jar ${PRODUCT_BUILD_DIR}\lib\yahoo_search.jar
  File /oname=htmlparser.jar ${PRODUCT_BUILD_DIR}\lib\htmlparser.jar

  DetailPrint "Installing Galleon apps"
  SetOutPath "$INSTDIR\apps"
  File ${PRODUCT_BUILD_DIR}\apps\music.jar
  File ${PRODUCT_BUILD_DIR}\apps\musicOrganizer.jar
  File ${PRODUCT_BUILD_DIR}\apps\photos.jar
  File ${PRODUCT_BUILD_DIR}\apps\rss.jar
  File ${PRODUCT_BUILD_DIR}\apps\shoutcast.jar
  File ${PRODUCT_BUILD_DIR}\apps\togo.jar
  File ${PRODUCT_BUILD_DIR}\apps\weather.jar
  File ${PRODUCT_BUILD_DIR}\apps\desktop.jar
  File ${PRODUCT_BUILD_DIR}\apps\email.jar
  File ${PRODUCT_BUILD_DIR}\apps\internet.jar
  File ${PRODUCT_BUILD_DIR}\apps\iTunes.jar
  File ${PRODUCT_BUILD_DIR}\apps\playlists.jar
  File ${PRODUCT_BUILD_DIR}\apps\podcasting.jar
  File ${PRODUCT_BUILD_DIR}\apps\movies.jar  
  IfFileExists "$INSTDIR\apps\camera.jar" 0 HME
  Delete "$INSTDIR\apps\camera.jar"
  
HME:
  DetailPrint "Installing HME support"
  SetOutPath "$INSTDIR\hme"
  IfFileExists "${PRODUCT_BUILD_DIR}\hme\launcher.txt" Skins
  File ${PRODUCT_BUILD_DIR}\hme\launcher.txt
  
Skins:
  DetailPrint "Installing skins"
  SetOutPath "$INSTDIR\skins"
  File ${PRODUCT_BUILD_DIR}\skins\galleon.gln
  File ${PRODUCT_BUILD_DIR}\skins\tivo.gln  

  DetailPrint "Installing Galleon documentation"
  SetOutPath "$INSTDIR"
  File ${PRODUCT_BUILD_DIR}\Readme.txt
  File ${PRODUCT_BUILD_DIR}\copying
  File ${PRODUCT_BUILD_DIR}\ThirdPartyLicenses.txt

  CreateDirectory "$INSTDIR\logs"
  CreateDirectory "$INSTDIR\data"
  
ClassPath:
  DetailPrint "Installing Galleon menu items"
  ; Create start menu links
  CreateDirectory "$SMPROGRAMS\${PRODUCT_NAME}"
  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\Readme.lnk" "$INSTDIR\Readme.txt"
  SetOutPath "$INSTDIR\lib"
  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\Configure.lnk" "$SYSDIR\javaw.exe" '-classpath ..\conf\;galleon.jar;log4j.jar;forms.jar;commons.jar;concurrent.jar;hibernate.jar;hme.jar;pja.jar;dom4j.jar org.lnicholls.galleon.gui.Galleon' "$INSTDIR\media\images\galleon.ico"

InstallService:
  DetailPrint "Installing ${PRODUCT_NAME} service"
  StrCmp ${STATUS} "success" End ; Service installed previously
  ; <name of service: startstop name> <name to display: display in SCM> <service type> <start type> <service's binary:filepath> <load order group: name>
  ; d:\galleon\bin\Wrapper.exe -s d:\galleon\conf\wrapper.conf
  nsSCM::Install /NOUNLOAD "${PRODUCT_NAME}" "${PRODUCT_NAME}" 272 2 '"$INSTDIR\bin\Wrapper.exe" -s "$INSTDIR\conf\wrapper.conf"' "" ""
  Pop $3 ; return error/success
  StrCmp $3 "success" End
  Pop $3 ; return GetLastError/tag
  MessageBox MB_ICONEXCLAMATION|MB_OK "The ${PRODUCT_NAME} service could not be installed: $3"
  Quit
End:  

SectionEnd

; Create shortcuts
Section "Desktop shortcut" desktop
  StrCmp ${STATUS} "error" End
  CreateShortCut "$DESKTOP\${PRODUCT_NAME}.lnk" "$SMPROGRAMS\${PRODUCT_NAME}\Configure.lnk" "" "$INSTDIR\images\galleon.ico"
End:
SectionEnd

; Create shortcuts
Section -AdditionalIcons
  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\Website.lnk" "${PRODUCT_WEB_SITE}"
  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

; Write Galleon uninstall info to the registry
Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\wrapper.exe" "" "$INSTDIR\wrapper.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayName" "$(^Name)"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayIcon" "$INSTDIR\bin\wrapper.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd

Section -startGalleon SEC02
  Call GetWindowsVersion
  Pop $7 ; OS
  StrCmp $7 "XP" 0 StartService
  DetailPrint "Configuring firewall"
  nsExec::Exec 'netsh firewall add allowedprogram "$INSTDIR\bin\Wrapper.exe" ${PRODUCT_NAME}'
  Pop $7 ; return error/success
  StrCmp $7 "0" 0 FirewallFailed
  nsExec::Exec 'netsh firewall add portopening UDP 5353 HME ENABLE ALL'
  Pop $7 ; return error/success
  StrCmp $7 "0" 0 FirewallFailed  
  nsExec::Exec 'netsh firewall add portopening TCP 7288 HME ENABLE ALL'
  Pop $7 ; return error/success
  StrCmp $7 "0" StartService
FirewallFailed:
  MessageBox MB_ICONEXCLAMATION|MB_OK "The firewall could not be configured: $7. When the ${PRODUCT_NAME} installation completes, configure your firewall to allow the Galleon service to use TCP port 7288."

StartService:
  ; Start Galleon service
  DetailPrint "Starting ${PRODUCT_NAME} service"
  nsSCM::Start /NOUNLOAD "${PRODUCT_NAME}"
  Pop $7 ; return error/success
  StrCmp $7 "success" Done
  MessageBox MB_ICONINFORMATION|MB_OK "The ${PRODUCT_NAME} service could not be started. Try to manually start Galleon using Control Panel/Adminstrative Tools/Services."
  ; Quit

Done:
  ; Check if Galleon service is starting
  DetailPrint "Activating ${PRODUCT_NAME} service"
  Sleep 10000
  DetailPrint "Initializing ${PRODUCT_NAME} server"
  Sleep 10000  
  DetailPrint "Loading ${PRODUCT_NAME} server"
  Sleep 10000    
  nsSCM::QueryStatus /NOUNLOAD "${PRODUCT_NAME}"
  Pop $2 ; return error/success
  Pop $3 ; return service status
  StrCmp $2 "success" End
  StrCpy ${STATUS} "error"
  MessageBox MB_ICONINFORMATION|MB_OK "The ${PRODUCT_NAME} service could not be started. Try to manually start Galleon using Control Panel/Adminstrative Tools/Services."
  ; Quit

End:
  StrCpy ${STATUS} "success"
SectionEnd

Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed from your computer."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove $(^Name) and all of its components?" IDYES +2
  Abort
FunctionEnd

; Uninstall Galleon
Section Uninstall
  ;http://nsis.sourceforge.net/archive/viewpage.php?pageid=385
  ; Is service already installed?
  nsSCM::QueryStatus /NOUNLOAD "${PRODUCT_NAME}"
  Pop ${STATUS} ; return error/success
  Pop $1 ; return service status
  StrCmp ${STATUS} "error" RemoveService
  StrCmp $1 "4" 0 RemoveService

  ; Stop the Galleon service
  DetailPrint "Stopping ${PRODUCT_NAME} service"
  nsSCM::Stop /NOUNLOAD "${PRODUCT_NAME}"
  Pop $6 ; return error/success
  StrCmp $6 "success" RemoveService
  MessageBox MB_ICONEXCLAMATION|MB_OK "The ${PRODUCT_NAME} service could not be stopped"
  Abort

RemoveService:
  ; Remove the Galleon service
  DetailPrint "Removing ${PRODUCT_NAME} service"
  nsSCM::Remove /NOUNLOAD "${PRODUCT_NAME}"
  Pop $0 ; return error/success
  StrCmp $0 "success" ServiceRemoved
  MessageBox MB_ICONEXCLAMATION|MB_OK "The ${PRODUCT_NAME} service could not be removed"
  Abort

ServiceRemoved:
  ; wait for JVM to stop and release resources
  Sleep 3000
  ; Check if service is really uninstalled; might lock files to be deleted
  nsSCM::QueryStatus /NOUNLOAD "${PRODUCT_NAME}"
  Pop $2 ; return error/success
  Pop $3 ; return service status
  StrCmp $2 "error" ConfigureFirewall ConfigureFirewall
  MessageBox MB_ICONEXCLAMATION|MB_OK "The ${PRODUCT_NAME} service could not be removed"
  Abort
  
ConfigureFirewall:
  DetailPrint "Configuring firewall"
  nsExec::Exec 'netsh firewall delete allowedprogram "$INSTDIR\bin\Wrapper.exe"'
  nsExec::Exec 'netsh firewall delete portopening UDP 5353'
  nsExec::Exec 'netsh firewall delete portopening TCP 7288'

DeleteFiles:
  ; Remove shortcuts
  DetailPrint "Removing ${PRODUCT_NAME} files"
  Delete "$SMPROGRAMS\${PRODUCT_NAME}\Uninstall.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_NAME}\Website.lnk"
  IfFileExists "$DESKTOP\${PRODUCT_NAME}.lnk" 0 Next
  Delete "$DESKTOP\${PRODUCT_NAME}.lnk"
Next:
  Delete "$SMPROGRAMS\${PRODUCT_NAME}\Readme.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_NAME}\Configure.lnk"

  ; Remove Galleon files
  RMDir "$SMPROGRAMS\${PRODUCT_NAME}"
  RMDir /r "$INSTDIR"

  ; Clean registry of uninstall info
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\wrapper.exe"
  SetAutoClose true
SectionEnd

; Determine if JRE or JDK is already installed
; http://nsis.sourceforge.net/archive/viewpage.php?pageid=268
Function CheckInstalledJRE
  Call DetectJRE
  Pop ${TEMP}
  StrCmp ${TEMP} "OK" NoDownloadJRE
  Pop ${TEMP2}
  StrCmp ${TEMP2} "None" NoFound FoundOld

FoundOld:
  Goto DownloadJRE

NoFound:
  Goto DownloadJRE

DownloadJRE:
  StrCpy ${DOWNLOAD_JRE_FLAG} "Download"
  Return

NoDownloadJRE:
  Pop ${TEMP2}
  DetailPrint "JRE found at : ${TEMP2}"
  StrCpy ${DOWNLOAD_JRE_FLAG} "NoDownload"
  Return

FunctionEnd

Function DetectJRE
  ; Find JRE in registry
  ;ReadRegStr ${TEMP2} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ;StrCmp ${TEMP2} "" DetectTry2
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" DetectTry2
  ;Goto GetJRE

  StrCpy ${TEMP2} "1.5.0"
  ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  StrCmp ${TEMP3} "" 0 GetJRE
  StrCpy ${TEMP2} "1.5.0"
  ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  StrCmp ${TEMP3} "" 0 GetJRE
  StrCpy ${TEMP2} "1.4.2_06"
  ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  StrCmp ${TEMP3} "" 0 GetJRE
  ;StrCpy ${TEMP2} "1.4.2_05"
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" 0 GetJRE
  ;StrCpy ${TEMP2} "1.4.2_04"
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" 0 GetJRE
  ;StrCpy ${TEMP2} "1.4.2_03"
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" 0 GetJRE

  ;ReadRegStr ${TEMP2} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ;StrCmp ${TEMP2} "" DetectTry2
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" DetectTry2
  Goto GetJRE

DetectTry2:
  ; Find JDK in registry
  ;ReadRegStr ${TEMP2} HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ;StrCmp ${TEMP2} "" NoFound
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" NoFound

  StrCpy ${TEMP2} "1.5.0"
  ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  StrCmp ${TEMP3} "" 0 GetJRE
  StrCpy ${TEMP2} "1.5.0"
  ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  StrCmp ${TEMP3} "" 0 GetJRE
  StrCpy ${TEMP2} "1.4.2_06"
  ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  StrCmp ${TEMP3} "" 0 GetJRE
  ;StrCpy ${TEMP2} "1.4.2_05"
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" 0 GetJRE
  ;StrCpy ${TEMP2} "1.4.2_04"
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" 0 GetJRE
  ;StrCpy ${TEMP2} "1.4.2_03"
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" 0 GetJRE
  
  ;ReadRegStr ${TEMP2} HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ;StrCmp ${TEMP2} "" NoFound
  ;ReadRegStr ${TEMP3} HKLM "SOFTWARE\JavaSoft\Java Development Kit\${TEMP2}" "JavaHome"
  ;StrCmp ${TEMP3} "" NoFound

GetJRE:
  ; Check that the right version is installed
  IfFileExists "${TEMP3}\bin\java.exe" 0 NoFound
  ; First compare major/minor versions
  ;StrCpy ${VAL1} ${TEMP2} 1
  ;StrCpy ${VAL2} ${JRE_VERSION} 1
  ;IntCmp ${VAL1} ${VAL2} 0 FoundOld FoundNew
  ;StrCpy ${VAL1} ${TEMP2} 1 2
  ;StrCpy ${VAL2} ${JRE_VERSION} 1 2
  ;IntCmp ${VAL1} ${VAL2} FoundNew FoundOld FoundNew
  Push ${TEMP2}
  Push ${JRE_VERSION}
  Call CompareVersions
  Pop $1
  StrCmp $1 "1" FoundNew NoFound

NoFound:
  Push "None"
  Push "NOK"
  Return

FoundOld:
  StrCpy ${JRE_PATH} ${TEMP2}
  Push ${TEMP2}
  Push "NOK"
  Return

FoundNew:
  StrCpy ${JRE_PATH} ${TEMP3}
  Push "${TEMP3}\bin\java.exe"
  Push "OK"
  Return

FunctionEnd

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Uninstall sutff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;====================================================
; StrStr - Finds a given string in another given string.
;               Returns -1 if not found and the pos if found.
;          Input: head of the stack - string to find
;                      second in the stack - string to find in
;          Output: head of the stack
;====================================================
Function StrStr
  Push $0
  Exch
  Pop $0 ; $0 now have the string to find
  Push $1
  Exch 2
  Pop $1 ; $1 now have the string to find in
  Exch
  Push $2
  Push $3
  Push $4
  Push $5

  StrCpy $2 -1
  StrLen $3 $0
  StrLen $4 $1
  IntOp $4 $4 - $3

  unStrStr_loop:
    IntOp $2 $2 + 1
    IntCmp $2 $4 0 0 unStrStrReturn_notFound
    StrCpy $5 $1 $3 $2
    StrCmp $5 $0 unStrStr_done unStrStr_loop

  unStrStrReturn_notFound:
    StrCpy $2 -1

  unStrStr_done:
    Pop $5
    Pop $4
    Pop $3
    Exch $2
    Exch 2
    Pop $0
    Pop $1
FunctionEnd

;-------------------------------------------------------------------------------
 ; CompareVersions
 ; input:
 ;    top of stack = existing version
 ;    top of stack-1 = needed version
 ; output:
 ;    top of stack = 1 if current version => neded version, else 0
 ; version is a string in format "xx.xx.xx.xx" (number of interger sections
 ; can be different in needed and existing versions)
 ; http://nsis.sourceforge.net/archive/viewpage.php?pageid=409

Function CompareVersions
   ; stack: existing ver | needed ver
   Exch $R0
   Exch
   Exch $R1
   ; stack: $R1|$R0

   Push $R1
   Push $R0
   ; stack: e|n|$R1|$R0

   ClearErrors
   loop:
      IfErrors VersionNotFound
      Strcmp $R0 "" VersionTestEnd

      Call ParseVersion
      Pop $R0
      Exch

      Call ParseVersion
      Pop $R1
      Exch

      IntCmp $R1 $R0 +1 VersionOk VersionNotFound
      Pop $R0
      Push $R0

   goto loop

   VersionTestEnd:
      Pop $R0
      Pop $R1
      Push $R1
      Push $R0
      StrCmp $R0 $R1 VersionOk VersionNotFound

   VersionNotFound:
      StrCpy $R0 "0"
      Goto end

   VersionOk:
      StrCpy $R0 "1"
end:
   ; stack: e|n|$R1|$R0
   Exch $R0
   Pop $R0
   Exch $R0
   ; stack: res|$R1|$R0
   Exch
   ; stack: $R1|res|$R0
   Pop $R1
   ; stack: res|$R0
   Exch
   Pop $R0
   ; stack: res
FunctionEnd

;---------------------------------------------------------------------------------------
 ; ParseVersion
 ; input:
 ;      top of stack = version string ("xx.xx.xx.xx")
 ; output:
 ;      top of stack   = first number in version ("xx")
 ;      top of stack-1 = rest of the version string ("xx.xx.xx")
Function ParseVersion
   Exch $R1 ; version
   Push $R2
   Push $R3

   StrCpy $R2 1
   loop:
      StrCpy $R3 $R1 1 $R2
      StrCmp $R3 "." loopend
      StrCmp $R3 "_" loopend
      StrLen $R3 $R1
      IntCmp $R3 $R2 loopend loopend
      IntOp $R2 $R2 + 1
      Goto loop
   loopend:
   Push $R1
   StrCpy $R1 $R1 $R2
   Exch $R1

   StrLen $R3 $R1
   IntOp $R3 $R3 - $R2
   IntOp $R2 $R2 + 1
   StrCpy $R1 $R1 $R3 $R2

   Push $R1

   Exch 2
   Pop $R3

   Exch 2
   Pop $R2

   Exch 2
   Pop $R1
FunctionEnd

;http://nsis.sourceforge.net/archive/viewpage.php?pageid=137
!define ALL_USERS
!include WinMessages.nsh

!ifdef ALL_USERS
  !define WriteEnvStr_RegKey \
     'HKLM "SYSTEM\CurrentControlSet\Control\Session Manager\Environment"'
!else
  !define WriteEnvStr_RegKey 'HKCU "Environment"'
!endif

#
# WriteEnvStr - Writes an environment variable
# Note: Win9x systems requires reboot
#
# Example:
#  Push "HOMEDIR"           # name
#  Push "C:\New Home Dir\"  # value
#  Call WriteEnvStr
#
Function WriteEnvStr
  Exch $1 ; $1 has environment variable value
  Exch
  Exch $0 ; $0 has environment variable name
  Push $2

  Call IsNT
  Pop $2
  StrCmp $2 1 WriteEnvStr_NT
    ; Not on NT
    StrCpy $2 $WINDIR 2 ; Copy drive of windows (c:)
    FileOpen $2 "$2\autoexec.bat" a
    FileSeek $2 0 END
    FileWrite $2 "$\r$\nSET $0=$1$\r$\n"
    FileClose $2
    SetRebootFlag true
    Goto WriteEnvStr_done

  WriteEnvStr_NT:
      WriteRegExpandStr ${WriteEnvStr_RegKey} $0 $1
      SendMessage ${HWND_BROADCAST} ${WM_WININICHANGE} \
        0 "STR:Environment" /TIMEOUT=5000

  WriteEnvStr_done:
    Pop $2
    Pop $1
    Pop $0
FunctionEnd

#
# un.DeleteEnvStr - Removes an environment variable
# Note: Win9x systems requires reboot
#
# Example:
#  Push "HOMEDIR"           # name
#  Call un.DeleteEnvStr
#
Function un.DeleteEnvStr
  Exch $0 ; $0 now has the name of the variable
  Push $1
  Push $2
  Push $3
  Push $4
  Push $5

  Call un.IsNT
  Pop $1
  StrCmp $1 1 DeleteEnvStr_NT
    ; Not on NT
    StrCpy $1 $WINDIR 2
    FileOpen $1 "$1\autoexec.bat" r
    GetTempFileName $4
    FileOpen $2 $4 w
    StrCpy $0 "SET $0="
    SetRebootFlag true

    DeleteEnvStr_dosLoop:
      FileRead $1 $3
      StrLen $5 $0
      StrCpy $5 $3 $5
      StrCmp $5 $0 DeleteEnvStr_dosLoop
      StrCmp $5 "" DeleteEnvStr_dosLoopEnd
      FileWrite $2 $3
      Goto DeleteEnvStr_dosLoop

    DeleteEnvStr_dosLoopEnd:
      FileClose $2
      FileClose $1
      StrCpy $1 $WINDIR 2
      Delete "$1\autoexec.bat"
      CopyFiles /SILENT $4 "$1\autoexec.bat"
      Delete $4
      Goto DeleteEnvStr_done

  DeleteEnvStr_NT:
    DeleteRegValue ${WriteEnvStr_RegKey} $0
    SendMessage ${HWND_BROADCAST} ${WM_WININICHANGE} \
      0 "STR:Environment" /TIMEOUT=5000

  DeleteEnvStr_done:
    Pop $5
    Pop $4
    Pop $3
    Pop $2
    Pop $1
    Pop $0
FunctionEnd

#
# [un.]IsNT - Pushes 1 if running on NT, 0 if not
#
# Example:
#   Call IsNT
#   Pop $0
#   StrCmp $0 1 +3
#     MessageBox MB_OK "Not running on NT!"
#     Goto +2
#     MessageBox MB_OK "Running on NT!"
#
!macro IsNT UN
Function ${UN}IsNT
  Push $0
  ReadRegStr $0 HKLM \
    "SOFTWARE\Microsoft\Windows NT\CurrentVersion" CurrentVersion
  StrCmp $0 "" 0 IsNT_yes
  ; we are not NT.
  Pop $0
  Push 0
  Return

  IsNT_yes:
    ; NT!!!
    Pop $0
    Push 1
FunctionEnd
!macroend
!insertmacro IsNT ""
!insertmacro IsNT "un."

; Section descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${jre} "The Java Runtime Environment version 1.5.0 is required for Galleon to run. It will only be installed if it is not detected on you system."
  !insertmacro MUI_DESCRIPTION_TEXT ${desktop} "Create a shortcut on your desktop to the Galleon configuration program."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

 ; GetWindowsVersion
 ;
 ; Based on Yazno's function, http://yazno.tripod.com/powerpimpit/
 ; Updated by Joost Verburg
 ;
 ; Returns on top of stack
 ;
 ; Windows Version (95, 98, ME, NT x.x, 2000, XP, 2003)
 ; or
 ; '' (Unknown Windows Version)
 ;
 ; Usage:
 ;   Call GetWindowsVersion
 ;   Pop $R0
 ;   ; at this point $R0 is "NT 4.0" or whatnot

 Function GetWindowsVersion

   Push $R0
   Push $R1

   ReadRegStr $R0 HKLM "SOFTWARE\Microsoft\Windows NT\CurrentVersion" CurrentVersion

   ;IfErrors 0 lbl_winnt
   StrCmp $R0 "" 0 lbl_winnt
   
   ; we are not NT
   ReadRegStr $R0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion" "VersionNumber"
   
   StrCpy $R1 $R0 1
   StrCmp $R1 '4' 0 lbl_error

   StrCpy $R1 $R0 3

   StrCmp $R1 '4.0' lbl_win32_95
   StrCmp $R1 '4.9' lbl_win32_ME lbl_win32_98

   lbl_win32_95:
     StrCpy $R0 '95'
   Goto lbl_done

   lbl_win32_98:
     StrCpy $R0 '98'
   Goto lbl_done

   lbl_win32_ME:
     StrCpy $R0 'ME'
   Goto lbl_done

   lbl_winnt:
   StrCpy $R1 $R0 1
   
   StrCmp $R1 '3' lbl_winnt_x
   StrCmp $R1 '4' lbl_winnt_x

   StrCpy $R1 $R0 3
   
   StrCmp $R1 '5.0' lbl_winnt_2000
   StrCmp $R1 '5.1' lbl_winnt_XP
   StrCmp $R1 '5.2' lbl_winnt_2003 lbl_error

   lbl_winnt_x:
     StrCpy $R0 "NT $R0" 6
   Goto lbl_done

   lbl_winnt_2000:
     Strcpy $R0 '2000'
   Goto lbl_done

   lbl_winnt_XP:
     Strcpy $R0 'XP'
   Goto lbl_done

   lbl_winnt_2003:
     Strcpy $R0 '2003'
   Goto lbl_done

   lbl_error:
     Strcpy $R0 ''
   lbl_done:

   Pop $R1
   Exch $R0

 FunctionEnd
