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

package com.redhat.thermostat.thread.client.controller.impl;

import com.redhat.thermostat.client.core.NameMatchingRefFilter;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.common.ApplicationCache;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.client.controller.ThreadInformationService;
import com.redhat.thermostat.thread.client.controller.impl.cache.AppCache;
import com.redhat.thermostat.thread.client.controller.impl.cache.AppCacheKey;
import com.redhat.thermostat.thread.dao.LockInfoDao;

public class ThreadInformationServiceImpl implements ThreadInformationService {
    
    private static final int ORDER = ORDER_THREAD_GROUP;
    
    private Filter<VmRef> filter = new NameMatchingRefFilter<>();
    private ApplicationService service;
    private VmInfoDAO vmInfoDao;
    private LockInfoDao lockInfoDao;
    private ThreadCollectorFactory collectorFactory;
    private ThreadViewProvider viewFactory;
    private ProgressNotifier notifier;

    public ThreadInformationServiceImpl(ApplicationService appService,
                                        VmInfoDAO vmInfoDao,
                                        LockInfoDao lockInfoDao,
                                        ThreadCollectorFactory collectorFactory,
                                        ThreadViewProvider viewFactory, ProgressNotifier notifier)
    {
        this.service = appService;
        this.vmInfoDao = vmInfoDao;
        this.lockInfoDao = lockInfoDao;
        this.collectorFactory = collectorFactory;
        this.viewFactory = viewFactory;
        this.notifier = notifier;
    }
    
    @Override
    public Filter<VmRef> getFilter() {
        return filter;
    }

    private AppCache getCache(VmRef ref) {
        ApplicationCache applicationCache = service.getApplicationCache();
        AppCache cache = (AppCache) applicationCache.getAttribute(ref.getVmId());
        if (cache == null) {
            AppCacheKey mainKey = new AppCacheKey(ref.getVmId(), ThreadInformationController.class);
            cache = new AppCache(mainKey, applicationCache);
            applicationCache.addAttribute(ref.getVmId(), cache);
        }
        return cache;
    }

    @Override
    public InformationServiceController<VmRef> getInformationServiceController(VmRef ref) {
        AppCache cache = getCache(ref);
        AppCacheKey key = new AppCacheKey(ref.getVmId(), ThreadInformationController.class);
        ThreadInformationController controller = cache.retrieve(key);
        if (controller == null) {
            controller = new ThreadInformationController(ref, service,
                                                         vmInfoDao,
                                                         lockInfoDao,
                                                         collectorFactory,
                                                         viewFactory,
                                                         notifier);
            cache.save(key, controller);
        }
        return controller;
    }

    @Override
    public int getOrderValue() {
        return ORDER;
    }

}

