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

package drbd.data;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import drbd.utilities.Tools;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class holds a set of all clusters.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Clusters {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Set of cluster objects. */
    private final Set<Cluster> clusters = new LinkedHashSet<Cluster>();
    /** Clusters set lock. */
    private final Mutex mClustersLock = new Mutex();

    /** Adds cluster to the set of clusters. */
    void addCluster(final Cluster cluster) {
        try {
            mClustersLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clusters.add(cluster);
        mClustersLock.release();
    }

    /** Removes cluster from the clusters. */
    void removeCluster(final Cluster cluster) {
        try {
            mClustersLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clusters.remove(cluster);
        mClustersLock.release();
    }

    /** Returns true if cluster is in the clusters or false if it is not. */
    boolean existsCluster(final Cluster cluster) {
        try {
            mClustersLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final boolean ret = clusters.contains(cluster);
        mClustersLock.release();
        return ret;
    }

    /** Gets set of clusters. */
    public Set<Cluster> getClusterSet() {
        final Set<Cluster> copy = new LinkedHashSet<Cluster>();
        try {
            mClustersLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final Cluster c : clusters) {
            copy.add(c);
        }
        mClustersLock.release();
        return copy;
    }

    /** Return default name with incremented index. */
    public String getDefaultClusterName() {
        int index = 0;
        final String defaultName = Tools.getString("Clusters.DefaultName");
        try {
            mClustersLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (clusters != null) {
            for (final Cluster cluster : clusters) {
            /* find the bigest index of cluster default name and increment it
             * by one */
                final String name = cluster.getName();
                final Pattern p = Pattern.compile("^"
                                                  + defaultName
                                                  + "(\\d+)$");
                final Matcher m = p.matcher(name);
                if (m.matches()) {
                    final int i = Integer.parseInt(m.group(1));
                    if (i > index) {
                        index = i;
                    }
                }
            }
        }
        mClustersLock.release();
        return defaultName + Integer.toString(index + 1);
    }
}
