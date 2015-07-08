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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.client.swing.components.ValueField;


public class SingleValueChartPanel extends JPanel {

    public static final String PROPERTY_VISIBLE_TIME_RANGE = "visibleTimeRange";

    private static final Color WHITE = new Color(255,255,255,0);
    private static final Color BLACK = new Color(0,0,0,0);
    private static final float TRANSPARENT = 0.0f;

    private static final int MINIMUM_DRAW_SIZE = 100;

    private JPanel chartsPanel;
    private List<ChartPanel> charts;

    private RecentTimeControlPanel recentTimeControlPanel;
    private JTextComponent label;

    private Duration initialDuration;

    public SingleValueChartPanel(JFreeChart chart, Duration initialDuration) {
        this(initialDuration);

        addChart(chart);
    }

    public SingleValueChartPanel(Duration initialDuration) {
        this.initialDuration = initialDuration;
        this.chartsPanel = new JPanel();
        this.charts = new ArrayList<>();

        chartsPanel.setLayout(new BoxLayout(chartsPanel, BoxLayout.Y_AXIS));

        this.setLayout(new BorderLayout());

        recentTimeControlPanel = new RecentTimeControlPanel(initialDuration);
        recentTimeControlPanel.addPropertyChangeListener(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                Duration d = (Duration) evt.getNewValue();
                SingleValueChartPanel.this.firePropertyChange(RecentTimeControlPanel.PROPERTY_VISIBLE_TIME_RANGE, null, d);
            }
        });

        add(chartsPanel, BorderLayout.CENTER);
        add(recentTimeControlPanel, BorderLayout.SOUTH);

        revalidate();
    }

    public void addChart(JFreeChart chart) {
        // jfreechart still generates tooltips when disabled, prevent generation as well
        if (chart.getPlot() instanceof XYPlot) {
            chart.getXYPlot().getRenderer().setBaseToolTipGenerator(null);
        }

        chart.getXYPlot().getRangeAxis().setAutoRange(true);

        chart.getXYPlot().getDomainAxis().setAutoRange(true);
        chart.getXYPlot().getDomainAxis().setFixedAutoRange(initialDuration.unit.toMillis(initialDuration.value));

        chart.getPlot().setBackgroundPaint(WHITE);
        chart.getPlot().setBackgroundImageAlpha(TRANSPARENT);
        chart.getPlot().setOutlinePaint(BLACK);

        ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.setDisplayToolTips(false);
        chartPanel.setDoubleBuffered(true);
        chartPanel.setMouseZoomable(false);
        chartPanel.setPopupMenu(null);

        /*
         * By default, ChartPanel scales itself instead of redrawing things when
         * it's resized. To have it resize automatically, we need to set minimum
         * and maximum sizes. Lets constrain the minimum, but not the maximum
         * size.
         */
        chartPanel.setMinimumDrawHeight(MINIMUM_DRAW_SIZE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawWidth(MINIMUM_DRAW_SIZE);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);

        chartsPanel.add(chartPanel);
        chartsPanel.revalidate();
        charts.add(chartPanel);

    }

    public void setTimeRangeToShow(int timeValue, TimeUnit timeUnit) {
        for (ChartPanel cp : charts) {
            XYPlot plot = cp.getChart().getXYPlot();

            // Don't drop old data; just dont' show it.
            plot.getDomainAxis().setAutoRange(true);
            plot.getDomainAxis().setFixedAutoRange(timeUnit.toMillis(timeValue));
        }
    }

    public void setDataInformationLabel(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label == null) {
                    label = new ValueField(text);
                    recentTimeControlPanel.addTextComponent(label);
                }

                label.setName("crossHair");
                label.setText(text);
            }
        });
    }

}

