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

package com.redhat.thermostat.client.swing.internal.views;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JListFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.core.views.AgentInformationDisplayView;
import com.redhat.thermostat.client.core.views.AgentInformationDisplayView.ConfigurationAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class AgentInformationDisplayFrameTest {

    private AgentInformationDisplayFrame agentConfigFrame;
    private FrameFixture fixture;
    private ActionListener<AgentInformationDisplayView.ConfigurationAction> l;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        agentConfigFrame = GuiActionRunner.execute(new GuiQuery<AgentInformationDisplayFrame>() {

            @Override
            protected AgentInformationDisplayFrame executeInEDT() throws Throwable {
                return new AgentInformationDisplayFrame();
            }
        });

        @SuppressWarnings("unchecked")
        ActionListener<AgentInformationDisplayView.ConfigurationAction> listener = mock(ActionListener.class);
        l = listener;
        agentConfigFrame.addConfigurationListener(l);

        fixture = new FrameFixture(agentConfigFrame.getFrame());
    }

    @After
    public void tearDown() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                agentConfigFrame.hideDialog();
            }
        });

        fixture.requireNotVisible();
        agentConfigFrame.removeConfigurationListener(l);

        fixture.cleanUp();
        fixture = null;
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testWindowClose() {
        fixture.show();

        fixture.close();

        verify(l).actionPerformed(eq(new ActionEvent<>(agentConfigFrame, AgentInformationDisplayView.ConfigurationAction.CLOSE)));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testClickOnCloseButton() {
        fixture.show();

        fixture.button("close").click();

        fixture.robot.waitForIdle();

        verify(l).actionPerformed(eq(new ActionEvent<>(agentConfigFrame, AgentInformationDisplayView.ConfigurationAction.CLOSE)));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testAddingAgentWorks() {
        fixture.show();
        JListFixture list = fixture.list("agentList");
        assertArrayEquals(new String[0], list.contents());

        agentConfigFrame.addAgent("test-agent");

        assertArrayEquals(new String[] { "test-agent" }, list.contents());
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testSelectingAgentWorks() {
        fixture.show();
        agentConfigFrame.addAgent("testAgent");
        JListFixture list = fixture.list("agentList");

        list.selectItem("testAgent");

        verify(l, atLeast(1)).actionPerformed(eq(new ActionEvent<>(agentConfigFrame, AgentInformationDisplayView.ConfigurationAction.SWITCH_AGENT)));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testFirstAddedAgentIsAutomaticallySelected() {
        fixture.show();
        agentConfigFrame.addAgent("testAgent");

        fixture.robot.waitForIdle();

        verify(l).actionPerformed(eq(new ActionEvent<>(agentConfigFrame, AgentInformationDisplayView.ConfigurationAction.SWITCH_AGENT)));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testRemovingAllAgentsWorks() {
        fixture.show();
        agentConfigFrame.addAgent("test-agent");
        JListFixture list = fixture.list("agentList");

        agentConfigFrame.clearAllAgents();

        assertArrayEquals(new String[0], list.contents());
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testInitialInformation() {
        fixture.show();

        String EMPTY_TEXT = "---";

        assertEquals(EMPTY_TEXT, fixture.textBox("agentName").text());
        assertEquals(EMPTY_TEXT, fixture.textBox("agentId").text());
        assertEquals(EMPTY_TEXT, fixture.textBox("commandAddress").text());
        assertEquals(EMPTY_TEXT, fixture.textBox("startTime").text());
        assertEquals(EMPTY_TEXT, fixture.textBox("stopTime").text());
        assertEquals(EMPTY_TEXT, fixture.textBox("backendDescription").text());

    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testUpdatingAgentInformationWorks() {

        final String AGENT_NAME = "the-agent-name";
        final String AGENT_ID = "the-agent-id";
        final String COMMAND_ADDRESS = "agent-command-channel-address";
        final String START_TIME = "some-start-time";
        final String STOP_TIME = "a-certain-stop-time";

        agentConfigFrame.setSelectedAgentName(AGENT_NAME);
        agentConfigFrame.setSelectedAgentId(AGENT_ID);
        agentConfigFrame.setSelectedAgentCommandAddress(COMMAND_ADDRESS);
        agentConfigFrame.setSelectedAgentStartTime(START_TIME);
        agentConfigFrame.setSelectedAgentStopTime(STOP_TIME);

        fixture.show();

        assertEquals(AGENT_NAME, fixture.textBox("agentName").text());
        assertEquals(AGENT_ID, fixture.textBox("agentId").text());
        assertEquals(COMMAND_ADDRESS, fixture.textBox("commandAddress").text());
        assertEquals(START_TIME, fixture.textBox("startTime").text());
        assertEquals(STOP_TIME, fixture.textBox("stopTime").text());
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testBackendDescriptionIsQueriedAndDisplayed() {
        final ActionEvent<ConfigurationAction> action = 
                new ActionEvent<>(agentConfigFrame, ConfigurationAction.SHOW_BACKEND_DESCRIPTION);
        final String[] BACKEND_NAMES = { "foo1", "foo2", "foo3" };
        final String[] BACKEND_STATUSES = { "bar1", "bar2", "bar3" };
        final String[] BACKEND_DESCRIPTIONS = { "baz1", "baz2", "baz3" };

        // Ordered by insertion
        Map<String, String> statusMap = new LinkedHashMap<>();
        statusMap.put(BACKEND_NAMES[0], BACKEND_STATUSES[0]);
        statusMap.put(BACKEND_NAMES[1], BACKEND_STATUSES[1]);
        statusMap.put(BACKEND_NAMES[2], BACKEND_STATUSES[2]);
        
        final Map<String, String> descMap = new HashMap<>();
        descMap.put(BACKEND_NAMES[0], BACKEND_DESCRIPTIONS[0]);
        descMap.put(BACKEND_NAMES[1], BACKEND_DESCRIPTIONS[1]);
        descMap.put(BACKEND_NAMES[2], BACKEND_DESCRIPTIONS[2]);
        
        // Add hook for ActionListener
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object arg = invocation.getArguments()[0];
                ActionEvent<?> event = (ActionEvent<?>) arg;
                String backendName = (String) event.getPayload();
                String description = descMap.get(backendName);
                agentConfigFrame.setSelectedAgentBackendDescription(description);
                return null;
            }
        }).when(l).actionPerformed(eq(action));

        fixture.show();

        agentConfigFrame.setSelectedAgentBackendStatus(statusMap);

        assertEquals(3, fixture.table("backends").rowCount());

        String[] rowContents = fixture.table("backends").contents()[0];
        assertArrayEquals(new String[] { BACKEND_NAMES[0], BACKEND_STATUSES[0] }, rowContents);
        rowContents = fixture.table("backends").contents()[1];
        assertArrayEquals(new String[] { BACKEND_NAMES[1], BACKEND_STATUSES[1] }, rowContents);
        rowContents = fixture.table("backends").contents()[2];
        assertArrayEquals(new String[] { BACKEND_NAMES[2], BACKEND_STATUSES[2] }, rowContents);

        // selectRows(0) doesn't trigger listener, since it is selected by default
        // so start with row 1
        fixture.table("backends").selectRows(1);
        fixture.robot.waitForIdle();
        assertEquals(BACKEND_DESCRIPTIONS[1], fixture.textBox("backendDescription").text());
        
        fixture.table("backends").selectRows(2);
        fixture.robot.waitForIdle();
        assertEquals(BACKEND_DESCRIPTIONS[2], fixture.textBox("backendDescription").text());
        
        fixture.table("backends").selectRows(0);
        fixture.robot.waitForIdle();
        assertEquals(BACKEND_DESCRIPTIONS[0], fixture.textBox("backendDescription").text());
    }

}

