/*
 * Copyright 2012 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.thread.client.swing.impl;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;

@SuppressWarnings("serial")
class ThreadAliveDaemonTimelinePanel extends JPanel {

    private static final Translate t = LocaleResources.createLocalizer();
    
    private JLabel liveThreads;
    private JLabel daemonThreads;
    private JPanel timelinePanel;

    /**
     * Create the panel.
     */
    public ThreadAliveDaemonTimelinePanel() {
        
        JPanel runningPanel = new JPanel();
        runningPanel.setOpaque(false);
        
        timelinePanel = new JPanel();
        timelinePanel.setOpaque(false);
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.X_AXIS));
        
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(runningPanel, GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
                        .addComponent(timelinePanel, GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(runningPanel, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(timelinePanel, GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                    .addGap(3))
        );
        
        JLabel liveThreadsLabel = new JLabel(t.localize(LocaleResources.LIVE_THREADS) + ":");
        
        JLabel daemonThreadsLabel = new JLabel(t.localize(LocaleResources.DAEMON_THREADS) + ":");
        
        liveThreads = new JLabel("-");
        liveThreads.setHorizontalAlignment(SwingConstants.RIGHT);
        
        daemonThreads = new JLabel("-");
        daemonThreads.setHorizontalAlignment(SwingConstants.RIGHT);
        GroupLayout gl_runningPanel = new GroupLayout(runningPanel);
        gl_runningPanel.setHorizontalGroup(
            gl_runningPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_runningPanel.createSequentialGroup()
                    .addGroup(gl_runningPanel.createParallelGroup(Alignment.TRAILING, false)
                        .addComponent(liveThreadsLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(daemonThreadsLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_runningPanel.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(daemonThreads, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(liveThreads, GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE))
                    .addContainerGap(221, Short.MAX_VALUE))
        );
        gl_runningPanel.setVerticalGroup(
            gl_runningPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_runningPanel.createSequentialGroup()
                    .addGroup(gl_runningPanel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(liveThreadsLabel)
                        .addComponent(liveThreads))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_runningPanel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(daemonThreadsLabel)
                        .addComponent(daemonThreads))
                    .addContainerGap(15, Short.MAX_VALUE))
        );
        runningPanel.setLayout(gl_runningPanel);
        setLayout(groupLayout);

    }
    
    public JLabel getLiveThreads() {
        return liveThreads;
    }
    
    public JLabel getDaemonThreads() {
        return daemonThreads;
    }
    
    public JPanel getTimelinePanel() {
        return timelinePanel;
    }
}
