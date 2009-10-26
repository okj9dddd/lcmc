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
 * Here are commands for fedora 11.
 */
public class DistResource_fedora_11 extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "fedora-11"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?fc\\d+).*"},

        {"DrbdInst.install",
         "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGE@ /tmp/drbdinst/@DRBDMODULEPACKAGE@"},

        /* Corosync/Openais/Pacemaker */
        {"PmInst.install.text.1",
         "opensuse:ha-clustering repo: 1.0.x/0.80.x" },

        {"PmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/Fedora_11/server:ha-clustering.repo && "
         + "(/usr/sbin/groupadd haclient 2>/dev/null && "
         + "/usr/sbin/useradd -g haclient hacluster 2>/dev/null;"
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9* install pacemaker"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi;"
         + " if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi)"
         + " && (/sbin/chkconfig --del heartbeat;"
         + " /sbin/chkconfig --level 2345 openais on"
         + " && /sbin/chkconfig --level 016 openais off)"},

        {"HbPmInst.install.text.1",
         "opensuse:ha-clustering repo: 1.0.x/2.99.x" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/ http://download.opensuse.org/repositories/server:/ha-clustering/Fedora_11/server:ha-clustering.repo && "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9* install heartbeat pacemaker && "
         + "/sbin/chkconfig --add heartbeat"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.method.2",
         "source"},

        {"DrbdInst.install.2",
         "/bin/mkdir -p /tmp/drbdinst && "
         + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
         + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
         //TODO: fedora11 has the it?
         /* it installs eather kernel-devel- or kernel-PAE-devel-, etc. */
         + "/usr/bin/yum -y install kernel`uname -r|"
         + " grep -o '\\.PAEdebug\\|\\.PAE'"
         + "|tr . -`-devel-`uname -r|sed 's/\\.\\(PAEdebug\\|PAE\\)$//'` "
         + "|tee -a /dev/tty|grep 'No package'>/dev/null;"
         + "(if [ \"$?\" == 0 ]; then "
         + "echo \"you need to find and install kernel-devel-`uname -r`.rpm "
         + "package, or upgrade the kernel, sorry\";"
         + "exit 1; fi)"
         + "&& /usr/bin/yum -y install flex gcc && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "make && make install && "
         //+ "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},
    };
}
