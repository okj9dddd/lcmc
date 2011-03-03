/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package drbd.configs;

import java.util.Arrays;

/**
 * Here are commands for all redhats.
 */
public final class DistResource_redhatenterpriseserver
                                        extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "redhatenterpriseserver"},
        {"arch:i686", "i686"},
        {"distribution", "rhel"},
        {"version:Red Hat Enterprise Linux ES release 4 (Nahant*", "4"},
        {"version:Red Hat Enterprise Linux Server release 5*", "5"},
        {"version:Red Hat Enterprise Linux Server release 6*", "6"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?el\\d+).*"},

        {"DrbdInst.install", "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},
        {"HbPmInst.install.text.1", "the redhat way: HB 2.1.4 (obsolete)"},
        {"HbPmInst.install.1", "/usr/bin/yum -y install heartbeat"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.method.2",
         "source"},

        {"DrbdInst.install.2",
         "/bin/mkdir -p /tmp/drbdinst && "
         + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
         + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "/usr/bin/yum -y install kernel`uname -r|"
          + " grep -o '5PAE\\|5xen\\|5debug'"
          + "|tr 5 -`-devel-`uname -r|sed 's/\\(PAE\\|xen\\|debug\\)$//'` && "
         + "/usr/bin/yum -y install glibc flex gcc && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         //+ "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},

        {"HbCheck.version",
         "@GUI-HELPER@ get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Heartbeat.deleteFromRc",
         "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         "/sbin/chkconfig --add heartbeat"},

        {"Corosync.addToRc",
         "/sbin/chkconfig --level 2345 corosync on "
         + "&& /sbin/chkconfig --level 016 corosync off"},

        {"Corosync.deleteFromRc",
         "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         "/sbin/chkconfig --level 2345 openais on "
         + "&& /sbin/chkconfig --level 016 openais off"},

        {"Openais.deleteFromRc",
         "/sbin/chkconfig --del openais"},
    };
}
