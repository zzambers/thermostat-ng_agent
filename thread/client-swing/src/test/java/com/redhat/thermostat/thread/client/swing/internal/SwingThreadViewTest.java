/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.swing.internal;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.ThreadView;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingThreadViewTest {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    
    private SwingThreadView view;
    
    private JFrame frame;
    private FrameFixture frameFixture;
    
    private static Locale locale;

    private static UIDefaults defaults;

    @BeforeClass
    public static void setUpOnce() {
        defaults = mock(UIDefaults.class);
        when(defaults.getReferenceFieldIconColor()).thenReturn(Color.BLACK);

        FailOnThreadViolationRepaintManager.install();
        locale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }
    
    @AfterClass
    public static void tearDownOnce() {
        Locale.setDefault(locale);
    }
    
    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }
    
    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new SwingThreadView(defaults);
                frame = new JFrame();
                frame.add(view.getUiComponent());
            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void verifyThreadCountIsFirstTab() {
        frameFixture.show();

        JTabbedPaneFixture tabPane = frameFixture.tabbedPane("topTabbedPane");
        Component comp = tabPane.component().getComponentAt(0);
        assertEquals("count", comp.getName());
        
        comp = tabPane.component().getComponentAt(1);
        assertEquals("timeline", comp.getName());
    }
    
    @GUITest
    @Test
    public void verifyMonitorLabelChange() throws InvocationTargetException, InterruptedException {
        frameFixture.show();

        JToggleButtonFixture togglefixture = frameFixture.toggleButton("recordButton");
        
        togglefixture.requireToolTip(t.localize(LocaleResources.START_RECORDING).getContents());

        togglefixture.click();

        togglefixture.requireToolTip(t.localize(LocaleResources.STOP_RECORDING).getContents());
        
        // now try "programmatically"
        
        view.setRecording(ThreadView.MonitoringState.STARTED, true);
        
        togglefixture.requireToolTip(t.localize(LocaleResources.STOP_RECORDING).getContents());
    
        view.setRecording(ThreadView.MonitoringState.STOPPED, false);
        
        togglefixture.requireToolTip(t.localize(LocaleResources.START_RECORDING).getContents());
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void verifyToggleButtonIsDisabled() {
        frameFixture.show();

        view.setEnableRecordingControl(false);

        JToggleButtonFixture togglefixture = frameFixture.toggleButton("recordButton");
        togglefixture.requireDisabled();
    }

}

