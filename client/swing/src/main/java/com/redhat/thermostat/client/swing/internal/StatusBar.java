/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.Transient;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.redhat.thermostat.shared.locale.LocalizedString;

/**
 * A status bar used to display relevant information and status.
 * <p>
 * A status bar is normally displayed at the bottom of the main window.
 * It may contain one or more areas displaying text and icons.
 */
@SuppressWarnings("serial")
public class StatusBar extends JPanel {

    // some of this code is inspired by the book
    // Swing Hacks: Tips & Tools for Building Killer GUIs
    // By Joshua Marinacci, Chris Adamson
    // ISBN: 0-596-00907-0
    // website: http://www.oreilly.com/catalog/swinghks/

    public static final String PRIMARY_STATUS_PROPERTY = "primaryStatus";
    
    private Dimension preferredSize;
    
    private LocalizedString primaryStatus = LocalizedString.EMPTY_STRING;
    private JLabel primaryStatusLabel;
    private JLabel iconLabel;
    
    public StatusBar() {
        super();
        setLayout(new BorderLayout(0, 0));
        
        primaryStatusLabel = new JLabel(primaryStatus.getContents());
        primaryStatusLabel.setName("primaryStatusLabel");
        primaryStatusLabel.setFont(getFont().deriveFont(10.0f));
        primaryStatusLabel.setHorizontalAlignment(JLabel.LEADING);
        primaryStatusLabel.setVerticalAlignment(JLabel.CENTER);

        add(primaryStatusLabel, BorderLayout.WEST);
        
        iconLabel = new JLabel("");
        ImageIcon grip = new ImageIcon(getClass().getResource("/icons/resize-grip.png"));
        iconLabel.setIcon(grip);

        iconLabel.setMinimumSize(new Dimension(grip.getIconWidth() + 1, grip.getIconHeight()));
        iconLabel.setPreferredSize(new Dimension(grip.getIconWidth() + 1, grip.getIconHeight()));
        iconLabel.setVerticalAlignment(JLabel.BOTTOM);

        add(iconLabel, BorderLayout.EAST);
        preferredSize = new Dimension(700, grip.getIconHeight() + 5);
    }
    
    @Override
    @Transient
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return preferredSize;
    }
    
    @Override
    @Transient
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return preferredSize;
    }
    
    void setPrimaryStatus(LocalizedString primaryStatus) {
        if (primaryStatus == null) throw new NullPointerException();
        
        String oldPrimaryStatus = this.primaryStatus.getContents();
        this.primaryStatus = primaryStatus;
        primaryStatusLabel.setText(" " + primaryStatus.getContents());
        
        firePropertyChange(PRIMARY_STATUS_PROPERTY, oldPrimaryStatus, this.primaryStatus.getContents());
        repaint();
    }

    LocalizedString getPrimaryStatus() {
        return primaryStatus;
    }

}

