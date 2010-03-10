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


package drbd.gui;

import drbd.utilities.Tools;
import drbd.data.Host;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.event.HierarchyEvent;



import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.HierarchyListener;

/**
 * @author rasto
 *
 * The very main panel, where everyting is inside.
 *
 */
public class MainPanel extends JPanel {

    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>MainPanel</code> object.
     */
    public MainPanel() {
        super(new BorderLayout());
        // TODO: not new Host() but null
        final TerminalPanel terminalPanel = new TerminalPanel(new Host());
        terminalPanel.setPreferredSize(new Dimension(
                    Short.MAX_VALUE,
                    Tools.getDefaultInt("MainPanel.TerminalPanelHeight")));
        terminalPanel.setMinimumSize(terminalPanel.getPreferredSize());
        terminalPanel.setMaximumSize(terminalPanel.getPreferredSize());
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                    new ClustersPanel(),
                                                    terminalPanel);
        Tools.getGUIData().setTerminalSplitPane(splitPane);

        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(1);
        splitPane.setOneTouchExpandable(true);

        splitPane.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(final HierarchyEvent e) {
                if ((e.getChangeFlags()
                     & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    Tools.getGUIData().expandTerminalSplitPane(1);
                }
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }
}
