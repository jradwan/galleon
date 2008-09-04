#!/bin/sh -x
# TODO: do this all in ant?
cd `dirname $0`/../..
sudo rm -rf osx-dir
mkdir osx-dir
mkdir -p osx-dir/Galleon/Galleon
(cd distro/osx/bundles && tar -cf - --exclude=\*~ --exclude=CVS *.app)|(cd osx-dir/Galleon/Galleon && tar xpBf - )
if [ $# -gt 0 ]; then
    rm -rf build.osx
    ant -Dplatform=osx package
    mv build build.osx
fi
    (cd build.osx && tar cf - *) | (cd  osx-dir/Galleon/Galleon/Galleon*.app/Contents/Resources && mkdir Java && cd Java && tar xpBf -) 
(cd osx-dir/Galleon/Galleon/Conf*.app/Contents/Resources && ln -s ../../../Galleon*.app/Contents/Resources/Java ./Java)
for i in osx-dir/Galleon/Galleon/*.app; do
    mkdir "$i"/Contents/MacOS
# You might be tempted to symlink this file, but that seems to make finder think both galleon & configure galleon are the same app.
##echo     cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub "$i"/Contents/MacOS/galleon_java_stub >"$i"/Contents/MacOS/ORIGIN.txt
## done in postflight now
##    cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub "$i"/Contents/MacOS/galleon_java_stub
    mkdir "$i"/Contents/Resources/Java/data/tmp
    /Developer/Tools/SetFile -a B "$i"
done
mv "osx-dir/Galleon/Galleon/Galleon Server.app"/Contents/Resources/Java/conf/configure.xml "osx-dir/Galleon/Galleon/Galleon Server.app"/Contents/Resources/Java/conf/configure.xml-default
#mkdir -p osx-dir/Install_resources/English.lproj
(cd distro/osx && tar --exclude=CVS --exclude=\*~ -cf - MainResources)|(cd osx-dir && tar xpBf -)
#cp distro/osx/MainResources/postflight osx-dir/Install_resources
#cp distro/osx/MainResources/postinstall osx-dir/Install_resources
cp ThirdPartyLicenses.txt osx-dir
cp copying osx-dir/COPYING
textutil -cat rtf -output osx-dir/MainResources/English.lproj/License.rtf distro/osx/License.fragment.rtf copying ThirdPartyLicenses.txt
textutil -cat rtf -output osx-dir/MainResources/English.lproj/ReadMe.rtf distro/osx/ReadMe.fragment.rtf build.osx/ReleaseNotes.txt distro/osx/ReadMe.lastfragment.rtf 
cp distro/osx/MainResources/English.lproj/Welcome.rtf osx-dir/MainResources/English.lproj
# ln -s /Applications osx-dir/Applications
sudo chown -R root:admin osx-dir
#/Developer/Tools/packagemaker -build -proj distro/osx/Galleon.pmproj -p distro/osx/Galleon.pkg -v
VER="2.5.3"

rm -rf Galleon.pkg
# Backward compat builds on 10.5, may need to update to native commands?
/Developer/usr/bin/packagemaker \
--root osx-dir/Galleon --target 10.4 --discard-forks --verbose \
--resources osx-dir/MainResources \
--info distro/osx/Info.plist \
--filter .DS_Store \
--out Galleon.pkg
# ??? -d distro/osx/Description.plist

VOL="Galleon"
FILES="Galleon.pkg"
DMG="tmp-$VOL.dmg"

SIZE=`du -sk ${FILES} | awk '{print $1}'`
SIZE=$((${SIZE}/1000+1))
# default is UDIF
hdiutil create "$DMG" -layout SPUD -megabytes ${SIZE} -ov -fs HFS+  -volname "${VOL}-${VER}"
DISK=`hdid "$DMG" | awk ' /Apple_partition_scheme/ {print $1} ' | awk -F/ '{print $3}'`
cp -R "${FILES}" "/Volumes/${VOL}-${VER}"
hdiutil eject $DISK

# convert to compressed image, delete temp image
rm -f "${VOL}-${VER}.dmg"
hdiutil convert "$DMG" -format UDZO -o "${VOL}-${VER}.dmg"
# Consider: 
hdiutil internet-enable -yes "${VOL}-${VER}.dmg"
rm -f "$DMG"

