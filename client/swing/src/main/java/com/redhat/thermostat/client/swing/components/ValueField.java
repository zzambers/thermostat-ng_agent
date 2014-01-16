/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components;

import java.awt.Color;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;

/**
 * A custom swing component meant for showing values. Use it like you would use
 * any other JTextComponent.
 */
@SuppressWarnings("serial")
public class ValueField extends JEditorPane {

    public ValueField(String text) {
        setUI(new ValueFieldUI());

        setText(text);
        setBorder(null);
        setOpaque(false);
        
        // TODO: we should cleanup those properties and define
        // Thermostat specific ones based on the actual look and feel, since
        // not all look and feel will necessarily have those set
        setBackground(UIManager.getColor("Label.background"));
        setForeground(UIManager.getColor("Label.foreground"));
        setSelectedTextColor(UIManager.getColor("Label.background"));
        
        Color selectionColor = UIManager.getColor("thermostat-selection-bg-color");
        if (selectionColor == null) {
            selectionColor = UIManager.getColor("Label.foreground");
        }
        setSelectionColor(selectionColor);

        setFont(UIManager.getFont("Label.font"));
        
        setEditable(false);
        
        /*
         * The default caret update policy forces any scroll pane this
         * component is added to to scroll so that this component is visible.
         * Normally, the caret is placed in the last instance of this
         * component created which is normally at the bottom of a scroll pane.
         * This forces the scroll pane to scroll to the bottom, unexpectedly.
         * This field is not meant to be editable in the first place so this
         * behaviour makes no sense; turn off the scroll updates.
         */
        ((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }
}

