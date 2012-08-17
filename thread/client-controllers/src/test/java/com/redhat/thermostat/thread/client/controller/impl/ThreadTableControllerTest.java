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

package com.redhat.thermostat.thread.client.controller.impl;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.thread.client.common.ThreadTableView;
import com.redhat.thermostat.thread.collector.ThreadCollector;
import com.redhat.thermostat.thread.collector.ThreadInfo;

public class ThreadTableControllerTest {

    private ThreadTableView view;
    private ThreadCollector collector;
    
    private Timer timer;
    
    private ActionListener<ThreadTableView.Action> actionListener;
    ArgumentCaptor<Runnable> timerActionCaptor;
    
    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        collector = mock(ThreadCollector.class);
        
        timer = mock(Timer.class);
                
        view = mock(ThreadTableView.class);
        
        setUpTimers();
    }
    
    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }
    
    private void setUpTimers() {
        timer = mock(Timer.class);
        timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());
    }
    
    @Test
    public void testStartThreadTableController() {
        
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());
        
        ThreadTableController controller = new ThreadTableController(view, collector, timer);
        controller.initialise();

        actionListener = viewArgumentCaptor.getValue();
        actionListener.actionPerformed(new ActionEvent<>(view, ThreadTableView.Action.VISIBLE));
        
        verify(timer).start();
        
        actionListener.actionPerformed(new ActionEvent<>(view, ThreadTableView.Action.HIDDEN));

        verify(timer).stop();
    }
}
