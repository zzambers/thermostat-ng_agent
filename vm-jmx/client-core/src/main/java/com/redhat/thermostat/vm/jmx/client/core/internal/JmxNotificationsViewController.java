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

package com.redhat.thermostat.vm.jmx.client.core.internal;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsViewProvider;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView.NotificationAction;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;

public class JmxNotificationsViewController implements InformationServiceController<VmRef> {

    private final JmxNotificationsView view;
    private final Timer timer;
    private final JmxNotificationDAO dao;
    private final AgentInfoDAO agentDAO;
    private final VmRef vm;

    private final AtomicBoolean toEnable = new AtomicBoolean(true);;

    public JmxNotificationsViewController(AgentInfoDAO agent, JmxNotificationDAO notification,
            TimerFactory timerFactory, final RequestQueue queue,
            JmxNotificationsViewProvider viewProvider,
            VmRef vmId) {
        this.dao = notification;
        this.agentDAO = agent;
        this.view = viewProvider.createView();
        this.timer = timerFactory.createTimer();
        this.vm = vmId;

        initializeTimer();

        view.addActionListener(new ActionListener<BasicView.Action>() {

            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case HIDDEN:
                    stopUpdatingView();
                    break;
                case VISIBLE:
                    startUpdatingView();
                    break;
                }
            }
        });

        view.addNotificationActionListener(new ActionListener<JmxNotificationsView.NotificationAction>() {
            @Override
            public void actionPerformed(ActionEvent<NotificationAction> actionEvent) {
                if (actionEvent.getActionId() == NotificationAction.TOGGLE_NOTIFICATIONS) {
                    new JmxToggleNotificationRequest(queue).sendEnableNotificationsRequestToAgent(vm, agentDAO, toEnable.get(),
                            new RequestResponseListener() {
                                @Override
                                public void fireComplete(Request request, Response response) {
                                    if (response.getType() == ResponseType.OK) {
                                        view.setNotificationsEnabled(toEnable.get());
                                        toEnable.set(!toEnable.get());
                                    }
                                }
                            });
                }
            }
        });

    }

    private void initializeTimer() {
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setInitialDelay(0);
        timer.setDelay(5);
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        timer.setAction(new Runnable() {

            private long lastTimeStamp = Long.MIN_VALUE;

            @Override
            public void run() {
                List<JmxNotification> notifications = dao.getNotifications(vm, lastTimeStamp);
                for (JmxNotification notification : notifications) {
                    lastTimeStamp = Math.max(lastTimeStamp, notification.getTimeStamp());
                    view.addNotification(notification);
                }
            }
        });

    }

    private void startUpdatingView() {
        timer.start();
    }

    private void stopUpdatingView() {
        timer.stop();
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public String getLocalizedName() {
        return "Notifications";
    }

}
