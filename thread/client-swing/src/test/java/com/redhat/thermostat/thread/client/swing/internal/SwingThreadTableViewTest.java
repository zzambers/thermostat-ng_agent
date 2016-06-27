/*
 * Copyright 2012-2016 Red Hat, Inc.
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
import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 */
@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingThreadTableViewTest {

    private SwingThreadTableView view;
    private JFrame frame;
    private FrameFixture frameFixture;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
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
                view = new SwingThreadTableView();
                frame = new JFrame();
                frame.add(view.getUiComponent());
            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @Bug(id = "3027",
         summary = "ThreadTableView displays the last thread states even when older sessions are selected",
         url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3027")
    @Test
    @Category(GUITest.class)
    @GUITest
    public void clearView() throws Exception {

        ThreadTableBean bean0 = mock(ThreadTableBean.class);

        frameFixture.show();

        view.display(bean0);
        view.submitChanges();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // just flush pending events
            }
        });

        assertTrue(view.getBeans().containsKey(bean0));
        assertTrue(view.getInfos().contains(bean0));

        view.clear();
        view.submitChanges();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // just flush pending events
            }
        });

        assertFalse(view.getBeans().containsKey(bean0));
        assertFalse(view.getInfos().contains(bean0));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void clearNotifyModelChange() throws Exception {

        frameFixture.show();

        final boolean [] result = new boolean[1];

        frameFixture.table("threadBeansTable").target.getModel().
                addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent e) {
                        result[0] = true;
                    }
                }
        );

        view.clear();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // just flush pending events
            }
        });

        assertTrue(result[0]);
    }
}
