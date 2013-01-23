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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapDumpDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;

public class HeapDumpDetailsController {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final Logger log = LoggingUtils.getLogger(HeapDumpDetailsController.class);

    private final ApplicationService appService;

    private HeapDumpDetailsView view;
    private HeapDump heapDump;
    private HeapHistogramViewProvider histogramViewProvider;
    private ObjectDetailsViewProvider objectDetailsViewProvider;
    private ObjectRootsViewProvider objectRootsViewProvider;

    public HeapDumpDetailsController(ApplicationService appService, HeapDumpDetailsViewProvider viewProvider, HeapHistogramViewProvider histogramProvider, ObjectDetailsViewProvider objectDetailsProvider, ObjectRootsViewProvider objectRootsProvider) {
        this.appService = appService;
        this.histogramViewProvider = histogramProvider;
        this.objectDetailsViewProvider = objectDetailsProvider;
        this.objectRootsViewProvider = objectRootsProvider;
        view = viewProvider.createView();
    }

    public void setDump(HeapDump dump) {
        this.heapDump = dump;
        try {
            HeapHistogramView heapHistogramView = histogramViewProvider.createView();
            heapHistogramView.display(heapDump.getHistogram());
            String title = translator.localize(LocaleResources.HEAP_DUMP_SECTION_HISTOGRAM);
            view.addSubView(title, heapHistogramView);
        } catch (IOException e) {
            log.log(Level.SEVERE, "unexpected error while reading heap dump", e);
        }

        ObjectDetailsController controller = new ObjectDetailsController(appService, dump, objectDetailsViewProvider, objectRootsViewProvider);
        ObjectDetailsView detailsView = controller.getView();
        view.addSubView(translator.localize(LocaleResources.HEAP_DUMP_SECTION_OBJECT_BROWSER), detailsView);

        // do a dummy search right now to prep the index
        heapDump.searchObjects("A_RANDOM_PATTERN", 1);
    }

    public BasicView getView() {
        return view;
    }

}

