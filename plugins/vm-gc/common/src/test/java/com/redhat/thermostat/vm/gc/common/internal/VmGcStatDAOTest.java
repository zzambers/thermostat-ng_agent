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

package com.redhat.thermostat.vm.gc.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.redhat.thermostat.vm.gc.common.internal.VmGcStatDAOImpl.HttpHelper;
import static com.redhat.thermostat.vm.gc.common.internal.VmGcStatDAOImpl.JsonHelper;

import java.util.Collection;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;
import com.redhat.thermostat.storage.core.Key;

public class VmGcStatDAOTest {

    private VmGcStat stat;
    private HttpClient httpClient;
    private HttpHelper httpHelper;
    private JsonHelper jsonHelper;
    private StringContentProvider contentProvider;
    private Request request;
    private ContentResponse response;
    private static final String AGENT_ID = "some-agent";
    private static final String JSON = "{\"this\":\"is\",\"also\":\"JSON\"}";

    @Before
    public void setup() throws Exception {
        stat = new VmGcStat();
        stat.setAgentId(AGENT_ID);
        stat.setTimeStamp(1234l);
        stat.setWallTime(4000l);
        stat.setRunCount(1000l);
        stat.setVmId("Vm-1");
        stat.setCollectorName("Collector");

        httpClient = mock(HttpClient.class);
        request = mock(Request.class);
        when(httpClient.newRequest(anyString())).thenReturn(request);
        response = mock(ContentResponse.class);
        when(response.getStatus()).thenReturn(HttpStatus.OK_200);
        when(request.send()).thenReturn(response);

        jsonHelper = mock(JsonHelper.class);
        when(jsonHelper.toJson(any(VmGcStat.class))).thenReturn(JSON);
        httpHelper = mock(HttpHelper.class);
        contentProvider = mock(StringContentProvider.class);
        when(httpHelper.createContentProvider(anyString())).thenReturn(contentProvider);
    }

    @Test
    public void verifyAddVmGcStat() throws Exception {
        VmGcStatDAO dao = new VmGcStatDAOImpl(httpClient, jsonHelper, httpHelper);

        dao.putVmGcStat(stat);

        final String url = VmGcStatDAOImpl.GATEWAY_URL + VmGcStatDAOImpl.GATEWAY_PATH;
        verify(httpClient).newRequest(url);
        verify(request).method(HttpMethod.POST);
        verify(jsonHelper).toJson(stat);
        verify(httpHelper).createContentProvider(JSON);
        verify(request).content(contentProvider, VmGcStatDAOImpl.CONTENT_TYPE);
        verify(request).send();
        verify(response).getStatus();
    }

    @Test
    public void testCategory() {
        assertEquals("vm-gc-stats", VmGcStatDAO.vmGcStatCategory.getName());
        Collection<Key<?>> keys = VmGcStatDAO.vmGcStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<String>("collectorName")));
        assertTrue(keys.contains(new Key<Long>("runCount")));
        assertTrue(keys.contains(new Key<Long>("wallTime")));
        assertEquals(6, keys.size());
    }


}

