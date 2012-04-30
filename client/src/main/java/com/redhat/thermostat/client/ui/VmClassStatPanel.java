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

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.model.DiscreteTimeData;

public class VmClassStatPanel extends JPanel implements VmClassStatView {

    private static final long serialVersionUID = 1067532168697544774L;

    private final TimeSeriesCollection dataset = new TimeSeriesCollection();

    private final ActionNotifier<Action> notifier = new ActionNotifier<Action>(this);

    public VmClassStatPanel() {
        // any name works
        dataset.addSeries(new TimeSeries("class-stat"));

        setBorder(Components.smallBorder());
        setLayout(new BorderLayout());

        add(Components.header(localize(LocaleResources.VM_LOADED_CLASSES)), BorderLayout.NORTH);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                localize(LocaleResources.VM_CLASSES_CHART_REAL_TIME_LABEL),
                localize(LocaleResources.VM_CLASSES_CHART_LOADED_CLASSES_LABEL),
                dataset,
                false, false, false);

        Component chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));

        add(chartPanel, BorderLayout.CENTER);

        addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(Component component) {
                notifier.fireAction(Action.HIDDEN);
            }
        });
    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void addClassCount(List<DiscreteTimeData<Long>> data) {
        final List<DiscreteTimeData<Long>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.getSeries(0);
                for (DiscreteTimeData<Long> data: copy) {
                    series.add(new FixedMillisecond(data.getTimeInMillis()), data.getData(), false);
                }
                series.fireSeriesChanged();
            }
        });

    }

    @Override
    public void clearClassCount() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.getSeries(0);
                series.clear();
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return this;
    }
}
