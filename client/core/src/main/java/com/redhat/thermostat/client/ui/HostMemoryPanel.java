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

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.internal.ui.swing.WrapLayout;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.utils.DisplayableValues;
import com.redhat.thermostat.common.utils.DisplayableValues.Scale;

public class HostMemoryPanel extends HostMemoryView implements SwingComponent {

    private JPanel visiblePanel;

    private final MemoryCheckboxListener memoryCheckboxListener = new MemoryCheckboxListener();

    private final JTextComponent totalMemory = new ValueField("${TOTAL_MEMORY}");

    private final JPanel memoryCheckBoxPanel = new JPanel(new WrapLayout(FlowLayout.LEADING));
    private final CopyOnWriteArrayList<GraphVisibilityChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final TimeSeriesCollection memoryCollection = new TimeSeriesCollection();
    private final Map<String, TimeSeries> dataset = new HashMap<>();
    private final Map<String, JCheckBox> checkBoxes = new HashMap<>();
    private final Map<String, Color> colors = new HashMap<>();

    private JFreeChart chart;

    public HostMemoryPanel() {
        super();
        initializePanel();

        visiblePanel.addHierarchyListener(new ComponentVisibleListener() {
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
    public void setTotalMemory(final String newValue) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                totalMemory.setText(newValue);
            }
        });
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public void addMemoryChart(final String tag, final String humanReadableName) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int colorIndex = colors.size();
                colors.put(tag, ChartColors.getColor(colorIndex));
                TimeSeries series = new TimeSeries(tag);
                dataset.put(tag, series);
                JCheckBox newCheckBox = new JCheckBox(createLabelWithLegend(humanReadableName, colors.get(tag)));
                newCheckBox.setActionCommand(tag);
                newCheckBox.setSelected(true);
                newCheckBox.addActionListener(memoryCheckboxListener);
                newCheckBox.setOpaque(false);
                checkBoxes.put(tag, newCheckBox);
                memoryCheckBoxPanel.add(newCheckBox);

                updateColors();
            }
        });

    }

    private String createLabelWithLegend(String text, Color color) {
        String hexColor = "#" + Integer.toHexString(color.getRGB() & 0x00ffffff);
        return "<html> <font color='" + hexColor + "'>\u2588</font> " + text + "</html>";
    }

    @Override
    public void removeMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.remove(tag);
                memoryCollection.removeSeries(series);
                JCheckBox box = checkBoxes.remove(tag);
                memoryCheckBoxPanel.remove(box);

                updateColors();
            }
        });
    }

    @Override
    public void showMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                memoryCollection.addSeries(series);

                updateColors();
            }
        });
    }

    @Override
    public void hideMemoryChart(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                memoryCollection.removeSeries(series);

                updateColors();
            }
        });
    }

    @Override
    public void addMemoryData(final String tag, List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final TimeSeries series = dataset.get(tag);
                for (DiscreteTimeData<? extends Number> timeData: copy) {
                    RegularTimePeriod period = new FixedMillisecond(timeData.getTimeInMillis());
                    if (series.getDataItem(period) == null) {
                        Long sizeInBytes = (Long) timeData.getData();
                        Double sizeInMegaBytes = DisplayableValues.Scale.convertTo(Scale.MiB, sizeInBytes);
                        series.add(new FixedMillisecond(timeData.getTimeInMillis()), sizeInMegaBytes, false);
                    }
                }
                series.fireSeriesChanged();
            }
        });
    }

    @Override
    public void clearMemoryData(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TimeSeries series = dataset.get(tag);
                series.clear();
            }
        });
    }

    @Override
    public void addGraphVisibilityListener(GraphVisibilityChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGraphVisibilityListener(GraphVisibilityChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    private void initializePanel() {
        visiblePanel = new JPanel();
        visiblePanel.setOpaque(false);

        chart = createMemoryChart();

        JPanel chartPanel = new RecentTimeSeriesChartPanel(new RecentTimeSeriesChartController(chart));
        chartPanel.setOpaque(false);

        JLabel lblMemory = Components.header(localize(LocaleResources.HOST_MEMORY_SECTION_OVERVIEW));

        JLabel totalMemoryLabel = Components.label(localize(LocaleResources.HOST_INFO_MEMORY_TOTAL));

        memoryCheckBoxPanel.setOpaque(false);

        GroupLayout groupLayout = new GroupLayout(visiblePanel);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, 883, Short.MAX_VALUE)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addComponent(totalMemoryLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(totalMemory, GroupLayout.DEFAULT_SIZE, 751, Short.MAX_VALUE))
                        .addComponent(lblMemory)
                        .addComponent(memoryCheckBoxPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblMemory)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(totalMemory, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(totalMemoryLabel))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(memoryCheckBoxPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        visiblePanel.setLayout(groupLayout);
    }

    private JFreeChart createMemoryChart() {
        // FIXME associate a fixed color with each type

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                localize(LocaleResources.HOST_MEMORY_CHART_TITLE), // Title
                localize(LocaleResources.HOST_MEMORY_CHART_TIME_LABEL), // x-axis Label
                localize(LocaleResources.HOST_MEMORY_CHART_SIZE_LABEL, Scale.MiB.name()), // y-axis Label
                memoryCollection, // Dataset
                false, // Show Legend
                false, // Use tooltips
                false // Configure chart to generate URLs?
                );

        chart.getPlot().setBackgroundPaint( new Color(255,255,255,0) );
        chart.getPlot().setBackgroundImageAlpha(0.0f);
        chart.getPlot().setOutlinePaint(new Color(0,0,0,0));

        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setAutoRangeMinimumSize(100);

        return chart;
    }

    private void fireShowHideHandlers(boolean show, String tag) {
        for (GraphVisibilityChangeListener listener: listeners) {
            if (show) {
                listener.show(tag);
            } else {
                listener.hide(tag);
            }
        }
    }

    /**
     * Adding or removing series to the series collection may change the order
     * of existing items. Plus the paint for the index is now out-of-date. So
     * let's walk through all the series and set the right paint for those.
     */
    private void updateColors() {
        XYItemRenderer itemRenderer = chart.getXYPlot().getRenderer();
        for (int i = 0; i < memoryCollection.getSeriesCount(); i++) {
            String tag = (String) memoryCollection.getSeriesKey(i);
            Color color = colors.get(tag);
            itemRenderer.setSeriesPaint(i, color);
        }
    }

    private class MemoryCheckboxListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            JCheckBox source = (JCheckBox) e.getSource();
            fireShowHideHandlers(source.isSelected(), source.getActionCommand());
        }

    }
}
