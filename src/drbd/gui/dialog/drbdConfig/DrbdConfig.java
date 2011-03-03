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


package drbd.gui.dialog.drbdConfig;

import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.dialog.WizardDialog;

/**
 * DrbdConfig super class from which all the drbd config wizards can be
 * extended. It just adds DrbdResourceInfo field.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public abstract class DrbdConfig extends WizardDialog {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Drbd resource info object. */
    private final DrbdResourceInfo dri;

    /** Prepares a new <code>DrbdConfig</code> object. */
    public DrbdConfig(final WizardDialog previousDialog,
                      final DrbdResourceInfo dri) {
        super(previousDialog);
        this.dri = dri;
    }

    /** Returns drbd resource info object. */
    protected final DrbdResourceInfo getDrbdResourceInfo() {
        return dri;
    }
}
