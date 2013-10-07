/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.Filter;
import com.redhat.thermostat.client.swing.components.SearchField;
import com.redhat.thermostat.client.ui.ContextAction;
import com.redhat.thermostat.client.ui.Decorator;
import com.redhat.thermostat.client.ui.DecoratorProvider;
import com.redhat.thermostat.client.ui.HostContextAction;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.HostsVMsLoader;

@RunWith(CacioFESTRunner.class)
public class MainWindowTest {

    private FrameFixture frameFixture;
    private MainWindow window;
    private ActionListener<MainView.Action> l;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @SuppressWarnings("unchecked") // mock(ActionListener.class)
    @Before
    public void setUp() {

        GuiActionRunner.execute(new GuiTask() {
            
            @Override
            protected void executeInEDT() throws Throwable {
                window = new MainWindow();
                l = mock(ActionListener.class);
                window.addActionListener(l);
            }
        });

        frameFixture = new FrameFixture(window);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
        window = null;
        l = null;
    }

    //@Category(GUITest.class)
    //@Test
    public void testHostVmDecoratorsAdded() throws InterruptedException {
        
        List<DecoratorProvider<HostRef>> decorators = new ArrayList<>();
        @SuppressWarnings("unchecked")
        DecoratorProvider<HostRef> refDecorator = mock(DecoratorProvider.class);
        final Decorator decorator = mock(Decorator.class);
        when(decorator.getLabel(anyString())).thenReturn("fluff");
        
        when(refDecorator.getDecorator()).thenReturn(decorator);
        
        @SuppressWarnings("unchecked")
        Filter<HostRef> filter = mock(Filter.class);
        when(filter.matches(isA(HostRef.class))).thenReturn(false).thenReturn(true);

        when(refDecorator.getFilter()).thenReturn(filter);
        
        decorators.add(refDecorator);
        
        HostsVMsLoader hostsVMsLoader = mock(HostsVMsLoader.class);
        Collection<HostRef> expectedHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));
        
        when(hostsVMsLoader.getHosts()).thenReturn(expectedHosts);
        
        //window.updateTree(null, null, decorators, null, hostsVMsLoader);

        Thread.sleep(50);
        
        frameFixture.show();
        frameFixture.requireVisible();
        
        verify(decorator, times(0)).getLabel("fluffhost1");
        verify(decorator, atLeastOnce()).getLabel("fluffhost2");
    }
    
//    @Category(GUITest.class)
//    @Test
//    public void testHostVMTreeFilterPropertySupport() {
//        String SEARCH_TEXT = "test";
//        frameFixture.show();
//        JTextComponentFixture hostVMTreeFilterField = frameFixture.textBox(SearchField.VIEW_NAME);
//        hostVMTreeFilterField.enterText(SEARCH_TEXT);
//
//        verify(l, times(SEARCH_TEXT.length())).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.HOST_VM_TREE_FILTER));
//    }

    @Category(GUITest.class)
    @Test
    public void verifyThatCloseFiresShutdownEvent() {

        frameFixture.show();

        frameFixture.close();
        frameFixture.requireNotVisible();
        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHUTDOWN));
    }

    @Category(GUITest.class)
    @Test
    public void verifyShowMainWindowShowsWindow() {
        window.showMainWindow();
        frameFixture.requireVisible();
    }

    @Category(GUITest.class)
    @Test
    public void verifyHideMainWindowHidesWindow() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window.showMainWindow();
            }
        });
        frameFixture.requireVisible();
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window.hideMainWindow();
            }
        });
        frameFixture.requireNotVisible();
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatClientPreferencesMenuItemTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("showClientConfig");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHOW_CLIENT_CONFIG));
    }

    @Category(GUITest.class)
    @Test
    public void verifyThatAgentPreferencesMenuItemTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("showAgentConfig");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SHOW_AGENT_CONFIG));
    }


    // FIXME: re-add when history mode is back
    //@Category(GUITest.class)
    //@Test
    public void verifyThatHistorySwitchTriggersEvent() {
        frameFixture.show();
        JMenuItemFixture menuItem = frameFixture.menuItem("historyModeSwitch");
        menuItem.click();
        frameFixture.close();
        frameFixture.requireNotVisible();

        verify(l).actionPerformed(new ActionEvent<MainView.Action>(window, MainView.Action.SWITCH_HISTORY_MODE));
    }

    @Category(GUITest.class)
    @Test
    public void addRemoveMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test2");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_NAME});
        when(action.getType()).thenReturn(MenuAction.Type.STANDARD);

        JMenuItemFixture menuItem;

        frameFixture.show();

        window.addMenu(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);
        menuItem.click();

        verify(action).execute();

        window.removeMenu(action);

        try {
            menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
            // should not reach here
            assertTrue(false);
        } catch (ComponentLookupException cle) {
            // expected
        }
    }
    
    @Category(GUITest.class)
    @Test
    public void addRadioMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_NAME});


        when(action.getType()).thenReturn(MenuAction.Type.RADIO);

        JMenuItemFixture menuItem;

        frameFixture.show();

        window.addMenu(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        assertTrue(menuItem.target instanceof JRadioButtonMenuItem);
    }
    
    @Category(GUITest.class)
    @Test
    public void addCheckBoxMenu() {
        final LocalizedString PARENT_NAME = new LocalizedString("File");
        final LocalizedString MENU_NAME = new LocalizedString("Test");
        MenuAction action = mock(MenuAction.class);
        when(action.getName()).thenReturn(MENU_NAME);
        when(action.getType()).thenReturn(MenuAction.Type.CHECK);
        when(action.getPath()).thenReturn(new LocalizedString[] {PARENT_NAME, MENU_NAME});


        JMenuItemFixture menuItem;

        frameFixture.show();

        window.addMenu(action);

        menuItem = frameFixture.menuItemWithPath(PARENT_NAME.getContents(), MENU_NAME.getContents());
        assertNotNull(menuItem);

        assertTrue(menuItem.target instanceof JCheckBoxMenuItem);
    }
    
//    @GUITest
//    @Test
//    public void verifyContextMenu() {
//        List<ContextAction> actions = new ArrayList<>();
//
//        HostContextAction action = mock(HostContextAction.class);
//        when(action.getName()).thenReturn(new LocalizedString("action"));
//        when(action.getDescription()).thenReturn(new LocalizedString("description of action"));
//        Filter allMatchingFilter = mock(Filter.class);
//        when(allMatchingFilter.matches(any(HostRef.class))).thenReturn(true);
//
//        actions.add(action);
//
//        frameFixture.show();
//
//        // add a second action listener to discard the 'show' event invoked on the first
//        l = mock(ActionListener.class);
//        window.addActionListener(l);
//
//        MouseEvent e = new MouseEvent(window, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), MouseEvent.BUTTON2_MASK, 0, 0, 0, 0, 1, true, MouseEvent.BUTTON2);
//
//        window.showContextActions(actions, e);
//
//        JMenuItemFixture hostActionMenuItem = frameFixture.menuItem("action");
//        hostActionMenuItem.click();
//
//        ArgumentCaptor<ActionEvent> actionEventCaptor = ArgumentCaptor.forClass(ActionEvent.class);
//        verify(l).actionPerformed(actionEventCaptor.capture());
//
//        ActionEvent actionEvent = actionEventCaptor.getValue();
//        assertEquals(window, actionEvent.getSource());
//        assertEquals(MainView.Action.HOST_VM_CONTEXT_ACTION, actionEvent.getActionId());
//        assertEquals(action, actionEvent.getPayload());
//    }

}

