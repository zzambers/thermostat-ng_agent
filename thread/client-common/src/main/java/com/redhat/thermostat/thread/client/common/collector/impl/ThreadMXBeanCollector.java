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

package com.redhat.thermostat.thread.client.common.collector.impl;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.collector.HarvesterCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public class ThreadMXBeanCollector implements ThreadCollector {
    
    private static final Logger logger = LoggingUtils.getLogger(ThreadMXBeanCollector.class);

    private AgentInfoDAO agentDao;
    private ThreadDao threadDao;
    private BundleContext context;
    private VmRef ref;

    public ThreadMXBeanCollector(BundleContext context, VmRef ref) {
        this.context = context;
        this.ref = ref;
    }

    public void setThreadDao(ThreadDao threadDao) {
        this.threadDao = threadDao;
    }

    public void setAgentInfoDao(AgentInfoDAO agentDao) {
        this.agentDao = agentDao;
    }

    Request createRequest() {
        HostRef targetHostRef = ref.getAgent();
        
        String address = agentDao.getAgentInformation(targetHostRef).getConfigListenAddress();
        String [] host = address.split(":");
        
        InetSocketAddress target = new InetSocketAddress(host[0], Integer.parseInt(host[1]));
        Request harvester = new Request(RequestType.RESPONSE_EXPECTED, target);

        harvester.setReceiver(HarvesterCommand.RECEIVER);
        
        return harvester;
    }
    
    @Override
    public boolean startHarvester() {
        
        Request harvester = createRequest();
        harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.START.name());
        harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getIdString());
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        
        harvester.addListener(new RequestResponseListener() {
            @Override
            public void fireComplete(Request request, Response response) {
                switch (response.getType()) {
                case OK:
                    result[0] = true;
                    break;
                default:
                    break;
                }
                latch.countDown();
            }
        });
        
        try {
            enqueueRequest(harvester);
            latch.await();
        } catch (CommandException e) {
            logger.log(Level.WARNING, "Failed to enqueue request", e);
        } catch (InterruptedException ignore) {}
        
        return result[0];
    }

    @Override
    public boolean stopHarvester() {
        
        Request harvester = createRequest();
        harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.STOP.name());
        harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getIdString());

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];

        harvester.addListener(new RequestResponseListener() {
            @Override
            public void fireComplete(Request request, Response response) {
                switch (response.getType()) {
                case OK:
                    result[0] = true;
                    break;
                default:
                    break;
                }
                latch.countDown();
            }
        });
        
        try {
            enqueueRequest(harvester);
            latch.await();
        } catch (CommandException e) {
            logger.log(Level.WARNING, "Failed to enqueue request", e);
        } catch (InterruptedException ignore) {}
        return result[0];
    }
    
    @Override
    public boolean isHarvesterCollecting() {
        Request harvester = createRequest();
        harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.IS_COLLECTING.name());
        harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getIdString());

        final CountDownLatch latch = new CountDownLatch(1);        
        final boolean[] result = new boolean[1];

        harvester.addListener(new RequestResponseListener() {
            @Override
            public void fireComplete(Request request, Response response) {
                switch (response.getType()) {
                case OK:
                    result[0] = true;
                    break;
                default:
                    break;
                }
                latch.countDown();
            }
        });
        
        try {
            enqueueRequest(harvester);
            latch.await();
        } catch (CommandException e) {
            logger.log(Level.WARNING, "Failed to enqueue request", e);
        } catch (InterruptedException ignore) {}
        return result[0];
    }
    
    @Override
    public ThreadSummary getLatestThreadSummary() {
        ThreadSummary summary = threadDao.loadLastestSummary(ref);
        if (summary == null) {
            // default to all 0
            summary = new ThreadSummary();
        }
        return summary;
    }
    
    @Override
    public List<ThreadSummary> getThreadSummary(long since) {
        List<ThreadSummary> summary = threadDao.loadSummary(ref, since);
        return summary;
    }
    
    @Override
    public List<ThreadSummary> getThreadSummary() {
        return getThreadSummary(0);
    }
    
    @Override
    public List<ThreadInfoData> getThreadInfo() {
        return getThreadInfo(0);
    }
    
    @Override
    public List<ThreadInfoData> getThreadInfo(long since) {
        return threadDao.loadThreadInfo(ref, since);
    }

    @Override
    public VMThreadCapabilities getVMThreadCapabilities() {
        
        VMThreadCapabilities caps = threadDao.loadCapabilities(ref);
        if (caps == null) {
            Request harvester = createRequest();
            harvester.setParameter(HarvesterCommand.class.getName(), HarvesterCommand.VM_CAPS.name());
            harvester.setParameter(HarvesterCommand.VM_ID.name(), ref.getIdString());
            
            final CountDownLatch latch = new CountDownLatch(1);
            harvester.addListener(new RequestResponseListener() {
                @Override
                public void fireComplete(Request request, Response response) {
                    latch.countDown();
                }
            });

        
            try {
                enqueueRequest(harvester);
                latch.await();
                // FIXME there is no guarantee that data is now present in storage
                caps = threadDao.loadCapabilities(ref);
            } catch (InterruptedException ignore) {
                caps = new VMThreadCapabilities();
            } catch (CommandException e) {
                logger.log(Level.WARNING, "Failed to enqueue request", e);
            }
        }
        return caps;
    }
    
    private void enqueueRequest(Request req) throws CommandException {
        ServiceReference ref = context.getServiceReference(RequestQueue.class.getName());
        if (ref == null) {
            throw new CommandException("Cannot access command channel");
        }
        RequestQueue queue = (RequestQueue) context.getService(ref);
        queue.putRequest(req);
        context.ungetService(ref);
    }
    
}

