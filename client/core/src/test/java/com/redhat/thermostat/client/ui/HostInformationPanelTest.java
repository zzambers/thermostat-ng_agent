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

import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.thermostat.client.core.views.BasicView;

public class HostInformationPanelTest {

    private HostInformationPanel panel;
    private JFrame frame;
    private FrameFixture window;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        panel = createHostInfoPanel();

        frame = GuiActionRunner.execute(new GuiQuery<JFrame>() {
            @Override
            protected JFrame executeInEDT() throws Throwable {
                JFrame jFrame = new JFrame();
                panel.getUiComponent().setName("panel");
                jFrame.add(panel.getUiComponent());
                return jFrame;
            }
        });

        window = new FrameFixture(frame);
        window.show();
    }

    @After
    public void tearDown() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                frame.dispose();
            }
        });
        window.cleanUp();
    }
    
    private HostInformationPanel createHostInfoPanel() {
        return GuiActionRunner.execute(new GuiQuery<HostInformationPanel>() {
            @Override
            protected HostInformationPanel executeInEDT() throws Throwable {
                return new HostInformationPanel();
            }
        });
    }

    @Test
    public void testAddTwice() throws InvocationTargetException, InterruptedException {
        BasicView mock1 = createHostInfoPanel();

        panel.addChildView("foo1", mock1);

        // The panel in test has no views added so the matcher with a tab count > 0 works
        // in order to select the right panel.
        window.panel("panel").tabbedPane(new TabbedPaneMatcher(JTabbedPane.class)).requireTabTitles("foo1");

        BasicView mock2 = createHostInfoPanel();
        panel.addChildView("foo2", mock2);

        window.panel("panel").tabbedPane(new TabbedPaneMatcher(JTabbedPane.class)).requireTabTitles("foo1", "foo2");
    }

    @Test
    public void testAddRemove() throws InvocationTargetException, InterruptedException {
        BasicView test1 = createHostInfoPanel();
        BasicView test2 = createHostInfoPanel();

        panel.addChildView("test1", test1);
        panel.addChildView("test2", test2);

        // The panel in test has no views added so the matcher with a tab count > 0 works
        // in order to select the right panel.
        window.panel("panel").tabbedPane(new TabbedPaneMatcher(JTabbedPane.class)).requireTabTitles("test1", "test2");

        panel.removeChildView("test1");

        window.panel("panel").tabbedPane(new TabbedPaneMatcher(JTabbedPane.class)).requireTabTitles("test2");
    }
}
