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

package com.redhat.thermostat.client.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jfree.chart.ChartPanel;

public class RecentTimeSeriesChartPanel extends JPanel {

    private static final long serialVersionUID = -1733906800911900456L;

    private final RecentTimeSeriesChartController controller;

    public RecentTimeSeriesChartPanel(RecentTimeSeriesChartController controller) {
        this.controller = controller;

        this.setLayout(new BorderLayout());

        ChartPanel cp = controller.getChartPanel();

        cp.setDisplayToolTips(false);
        cp.setDoubleBuffered(true);
        cp.setMouseZoomable(false);
        cp.setPopupMenu(null);

        add(cp);
        add(getChartControls(), BorderLayout.SOUTH);
    }

    private Component getChartControls() {
        JPanel container = new JPanel();

        final JTextField durationSelector = new JTextField(5);
        final JComboBox unitSelector = new JComboBox(controller.getTimeUnits());

        int defaultValue = controller.getTimeValue();
        TimeUnit defaultUnit = controller.getTimeUnit();

        TimeUnitChangeListener timeUnitChangeListener = new TimeUnitChangeListener(controller, defaultValue, defaultUnit);

        durationSelector.getDocument().addDocumentListener(timeUnitChangeListener);
        unitSelector.addActionListener(timeUnitChangeListener);

        durationSelector.setText(String.valueOf(defaultValue));
        unitSelector.setSelectedItem(defaultUnit);

        container.add(new JLabel("Display the most recent"));
        container.add(durationSelector);
        container.add(unitSelector);

        return container;
    }

    private static class TimeUnitChangeListener implements DocumentListener, ActionListener {

        private final RecentTimeSeriesChartController controller;
        private int value;
        private TimeUnit unit;

        public TimeUnitChangeListener(RecentTimeSeriesChartController controller, int defaultValue, TimeUnit defaultUnit) {
            this.controller = controller;
            this.value = defaultValue;
            this.unit = defaultUnit;
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            changed(event.getDocument());
        }

        private void changed(Document doc) {
            try {
                this.value = Integer.valueOf(doc.getText(0, doc.getLength()));
            } catch (NumberFormatException nfe) {
                // ignore
            } catch (BadLocationException ble) {
                // ignore
            }
            updateChartParameters();
        }

        private void updateChartParameters() {
            controller.setTime(value, unit);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox comboBox = (JComboBox) e.getSource();
            TimeUnit time = (TimeUnit) comboBox.getSelectedItem();
            this.unit = time;
            updateChartParameters();
        }
    }
}
