# $Id$
#
#  Makefile for Galleon 2.x
#
# --
#
#


#
#  Installation Directories.
#
APPDIR      = /usr/share/galleon
CACHEDIR    = /var/cache/galleon
CONFDIR     = /etc/galleon
LIBDIR      = /usr/lib/galleon
LOGDIR      = /var/log/galleon
VARDIR      = /var/lib/galleon


#
#  Default target, give the user some help.
#
a:
	@echo "galleon 2.x Makefile"
	@echo ""
	@echo " Available targets (alphabetically):"
	@echo "    make install   - Install the software in ${APPDIR}."
	@echo "    make uninstall - Removes this software completely."
	@echo "    make upgrade   - Install the software in ${APPDIR} while saving the configure.xml that is currently installed."
	@echo " "
	@echo " For more details see:"
	@echo "    http://galleon.tv/"
	@echo " "

install:
	install -d ${APPDIR}
	install -d ${CACHEDIR}
	install -d ${CONFDIR}
	install -d ${LIBDIR}
	install -d ${LOGDIR}
	install -d ${VARDIR}/hme
	install -d ${VARDIR}/data
	cp -rf bin media ${APPDIR}
	cp -f Makefile COPYING *.txt ${APPDIR}
	cp -f conf/*.* ${CONFDIR}
	cp -rf lib/* ${LIBDIR}
	cp -rf apps skins ${VARDIR}
	cp -rf conf/templates ${VARDIR}
	chmod a+rwx ${APPDIR}/bin/run.sh
	chmod a+rwx ${APPDIR}/bin/gui.sh
	chmod a+rwx ${APPDIR}/bin/galleon
	chmod a+rwx ${APPDIR}/bin/wrapper*
	chmod +rw ${CONFDIR}/configure.xml
	ln -sf ${VARDIR}/apps ${APPDIR}/apps
	ln -sf ${CONFDIR} ${APPDIR}/conf
	ln -sf ${VARDIR}/data ${APPDIR}/data
	ln -sf ${VARDIR}/hme ${APPDIR}/hme 
	ln -sf ${LIBDIR} ${APPDIR}/lib
	ln -sf ${LOGDIR} ${APPDIR}/logs 
	ln -sf ${VARDIR}/skins ${APPDIR}/skins
	ln -sf ${VARDIR}/templates ${CONFDIR}/templates
	ln -sf ${APPDIR}/bin/galleon /etc/init.d/galleon
	chkconfig --add galleon

uninstall:
	chkconfig --del galleon
	rm -f ${APPDIR}/apps
	rm -f ${APPDIR}/conf
	rm -f ${APPDIR}/data
	rm -f ${APPDIR}/hme
	rm -f ${APPDIR}/lib
	rm -f ${APPDIR}/logs
	rm -f ${APPDIR}/skins
	rm -f ${CONFDIR}/templates
	rm -f /etc/init.d/galleon
	-rm -rf ${CONFDIR}
	-rm -rf ${LOGDIR}
	-rm -rf ${CACHEDIR}
	-rm -rf ${LIBDIR}
	-rm -rf ${VARDIR}
	-rm -rf ${APPDIR}

backup:
	cp -f ${CONFDIR}/configure.xml /tmp/configure.xml.save
	
restore:
	mv -f /tmp/configure.xml.save ${CONFDIR}/configure.xml
	
upgrade: backup uninstall install restore
