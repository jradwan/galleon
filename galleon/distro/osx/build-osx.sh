#!/bin/bash -x
# TODO: do this all in ant?
cd $(dirname $0)/../..
sudo rm -rf osx-dir
mkdir osx-dir
mkdir -p osx-dir/Galleon/Galleon
chmod 775 osx-dir/Galleon/Galleon
(cd distro/osx/bundles && tar -cf - --exclude=\*~ --exclude=CVS *.app)|(cd osx-dir/Galleon/Galleon && tar xpBf - )
if [ $# -gt 0 ]; then
    rm -rf build.osx
    ant -Dplatform=osx package
    mv build build.osx
fi
    (cd build.osx && tar cf - *) | (cd  "osx-dir/Galleon/Galleon/Galleon Server.app"/Contents/Resources && mkdir Java && cd Java && tar xpBf -) 
(cd osx-dir/Galleon/Galleon/Conf*.app/Contents/Resources && ln -s ../../../Galleon*.app/Contents/Resources/Java ./Java)
for i in "osx-dir/Galleon/Galleon/Galleon Server.app" "osx-dir/Galleon/Galleon/Configure Galleon.app"; do
    mkdir "$i"/Contents/MacOS
# You might be tempted to symlink this file, but that seems to make finder think both galleon & configure galleon are the same app.
##echo     cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub "$i"/Contents/MacOS/galleon_java_stub >"$i"/Contents/MacOS/ORIGIN.txt
## done in postflight now
##    cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub "$i"/Contents/MacOS/galleon_java_stub
    mkdir -p "$i"/Contents/Resources/Java/data/tmp
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
# ln -s /Applications/Galleon osx-dir/Galleon
sudo chown -R root:admin osx-dir
#/Developer/Tools/packagemaker -build -proj distro/osx/Galleon.pmproj -p distro/osx/Galleon.pkg -v
VER="2.5.6"

rm -rf Galleon.pkg Galleon.mpkg
# Be very wary, the package builder/installer on 10.5 is too clever.
# Installer will try to install over one of the .app bundles in
# the sources, not in /Applications, if it thinks that's a better place.
# And making a pmproj file seems to be unable to save the non-relocatable
# checkbox if it uses the relative paths.  So we have to copy a prototype
# project (with /path/to/galleon absolute paths) into a build-time copy
# recast with current on-disk paths.

srcdir=distro/osx/Galleon.pmdoc
blddir=distro/osx/Galleon-build.pmdoc

rm -rf $blddir
mkdir -p $blddir
mydir=$(pwd)
sed -e s:/path/to/galleon:${mydir}:g $srcdir/01galleon-contents.xml > $blddir/01galleon-contents.xml
sed -e s:/path/to/galleon:${mydir}:g $srcdir/01galleon.xml > $blddir/01galleon.xml
sed -e s:/path/to/galleon:${mydir}:g $srcdir/index.xml > $blddir/index.xml
/Developer/Tools/SetFile -a B $blddir

/Developer/usr/bin/packagemaker --doc $blddir -o Galleon.pkg -v

#rm -rf $blddir


VOL="Galleon"
FILES="Galleon.pkg"
DMG="tmp-$VOL.dmg"

SIZE=$(du -sk ${FILES} | awk '{print $1}')
SIZE=$((${SIZE}/1000+1))
# default is UDIF
hdiutil create "$DMG" -layout SPUD -megabytes ${SIZE} -ov -fs HFS+  -volname "${VOL}-${VER}"
DISK=$(hdid "$DMG" | awk ' /Apple_partition_scheme/ {print $1} ' | awk -F/ '{print $3}')
cp -R "${FILES}" "/Volumes/${VOL}-${VER}"
hdiutil eject $DISK

# convert to compressed image, delete temp image
rm -f "${VOL}-${VER}.dmg"
hdiutil convert "$DMG" -format UDZO -o "${VOL}-${VER}.dmg"
# Consider: 
hdiutil internet-enable -yes "${VOL}-${VER}.dmg"
rm -f "$DMG"

