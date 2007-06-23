#!/bin/sh -x
# TODO: do this all in ant?
cd `dirname $0`/../..
sudo rm -rf osx-dir
mkdir osx-dir
mkdir -p osx-dir/ROOT
(cd distro/osx/bundles && tar -cf - --exclude=\*~ --exclude=CVS *.app)|(cd osx-dir/ROOT && tar xpBf - )
if [ $# -gt 0 ]; then
    ant -Dplatform=osx package
fi
    (cd build && tar cf - *) | (cd  osx-dir/ROOT/Galleon*.app/Contents/Resources && mkdir Java && cd Java && tar xpBf -) 
(cd osx-dir/ROOT/Conf*.app/Contents/Resources && ln -s ../../../Galleon*.app/Contents/Resources/Java ./Java)
for i in osx-dir/ROOT/*.app; do
    mkdir "$i"/Contents/MacOS
# You might be tempted to symlink this file, but that seems to make finder think both galleon & configure galleon are the same app.
##echo     cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub "$i"/Contents/MacOS/galleon_java_stub >"$i"/Contents/MacOS/ORIGIN.txt
## done in postflight now
##    cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub "$i"/Contents/MacOS/galleon_java_stub
    mkdir "$i"/Contents/Resources/Java/data/tmp
    /Developer/Tools/SetFile -a B "$i"
done
mv "osx-dir/ROOT/Galleon Server.app"/Contents/Resources/Java/conf/configure.xml "osx-dir/ROOT/Galleon Server.app"/Contents/Resources/Java/conf/configure.xml-default
#mkdir -p osx-dir/Install_resources/English.lproj
(cd distro/osx && tar --exclude=CVS --exclude=\*~ -cf - MainResources)|(cd osx-dir && tar xpBf -)
#cp distro/osx/MainResources/postflight osx-dir/Install_resources
#cp distro/osx/MainResources/postinstall osx-dir/Install_resources
cp ThirdPartyLicenses.txt osx-dir
cp copying osx-dir/COPYING
cat <<EOF >osx-dir/License.txt
This Software includes original code under the following license
and additional code under licenses shown after the GPL (search for ----).

EOF
cat copying >>osx-dir/License.txt
cat ThirdPartyLicenses.txt >> osx-dir/License.txt
cp distro/osx/ReadMe.fragment.txt osx-dir/ReadMe.txt
cat build/ReleaseNotes.txt >>osx-dir/ReadMe.txt
textutil -convert rtf osx-dir/ReadMe.txt
textutil -convert rtf osx-dir/License.txt
cp distro/osx/MainResources/English.lproj/Welcome.rtf osx-dir
cp osx-dir/*.rtf osx-dir/MainResources/English.lproj
# ln -s /Applications osx-dir/Applications
sudo chown -R root:admin osx-dir
#/Developer/Tools/packagemaker -build -proj distro/osx/Galleon.pmproj -p distro/osx/Galleon.pkg -v
VER="2.4.0"

/Developer/Tools/packagemaker -build \
-f osx-dir/ROOT -ds -v -r osx-dir/MainResources \
-i distro/osx/Info.plist \
-d distro/osx/Description.plist -p Galleon.pkg -v

VOL="Galleon"
FILES="Galleon.pkg"
DMG="tmp-$VOL.dmg"

SIZE=`du -sk ${FILES} | awk '{print $1}'`
SIZE=$((${SIZE}/1000+1))
# default is UDIF
hdiutil create "$DMG" -megabytes ${SIZE} -ov -fs HFS+  -volname "${VOL}-${VER}"
DISK=`hdid "$DMG" | awk ' /Apple_partition_scheme/ {print $1} ' | awk -F/ '{print $3}'`
cp -R "${FILES}" "/Volumes/${VOL}-${VER}"
hdiutil eject $DISK

# convert to compressed image, delete temp image
rm -f "${VOL}-${VER}.dmg"
hdiutil convert "$DMG" -format UDZO -o "${VOL}-${VER}.dmg"
# Consider: 
hdiutil internet-enable -yes "${VOL}-${VER}.dmg"
rm -f "$DMG"

