#!/bin/sh -x
VERS=2
MINOR=5
SUB=0
OVERS=2
OMINOR=4
OSUB=2
for i in \
  distro/win32/galleon.nsi \
  distro/unix/build-zipfile.sh \
  src/org/lnicholls/galleon/Server/Constants.java \
  conf/configure.xml.default \
  distro/osx/build-osx.sh \
  "distro/osx/bundles/Configure Galleon.app/Contents/Info.plist" \
  "distro/osx/bundles/Galleon Server.app/Contents/Info.plist" \
  distro/osx/Info.plist \
; do
    sed -e s:$OVERS\\.$OMINOR\\.$OSUB:$VERS.$MINOR.$SUB:g \
	-e "/CURRENT_VERSION/s:$OVERS, $OMINOR, $OSUB:$VERS, $MINOR, $SUB:" \
	-e "/IFMinorVersion/s:integer>$OMINOR<:integer>$MINOR<:" \
	"$i" >"$i.new"
    diff -c "$i" "$i.new"
    if [ $# -gt 0 ]; then
	cp "$i.new" "$i"
	rm "$i.new"
    fi
done
