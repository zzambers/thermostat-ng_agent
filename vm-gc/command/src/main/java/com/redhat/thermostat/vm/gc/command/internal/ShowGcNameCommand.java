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

package com.redhat.thermostat.vm.gc.command.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper;
import com.redhat.thermostat.vm.gc.common.GcCommonNameMapper.CollectorCommonName;
import com.redhat.thermostat.vm.gc.common.params.GcParamsMapper;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.params.GcParam;
import com.redhat.thermostat.vm.gc.common.params.JavaVersionRange;

public class ShowGcNameCommand extends AbstractCommand {

    private static final Logger logger = LoggingUtils.getLogger(ShowGcNameCommand.class);

    // The name as which this command is registered.
    static final String REGISTER_NAME = "show-gc-name";
    static final String WITH_TUNABLES_FLAG ="with-tunables";
    static final String SHOW_TUNABLES_DESCRIPTIONS_FLAG ="show-tunables-descriptions";
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final GcCommonNameMapper commonNameMapper = new GcCommonNameMapper();
    private static final GcParamsMapper paramsMapper = GcParamsMapper.getInstance();
    private final DependencyServices services = new DependencyServices();
    private VmInfoDAO vmInfoDao;
    private VmGcStatDAO gcDao;
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        this.vmInfoDao = services.getRequiredService(VmInfoDAO.class,
                translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        this.gcDao = services.getRequiredService(VmGcStatDAO.class,
                translator.localize(LocaleResources.GC_STAT_DAO_SERVICE_UNAVAILABLE));

        Arguments arguments = ctx.getArguments();

        VmArgument vmArgument = VmArgument.required(arguments);
        VmId vmId = vmArgument.getVmId();
        VmInfo vmInfo = checkVmExists(vmId, translator.localize(LocaleResources.VM_NOT_FOUND, vmId.get()));

        PrintStream out = ctx.getConsole().getOutput();
        CollectorCommonName commonName = getCommonName(vmId);
        String msg = translator.localize(LocaleResources.GC_COMMON_NAME_SUCCESS_MSG,
                                      vmInfo.getVmId(),
                                      vmInfo.getMainClass(),
                                      commonName.getHumanReadableString())
                                      .getContents();
        out.println(msg);

        boolean withTunables = arguments.hasArgument(WITH_TUNABLES_FLAG);
        boolean showDescriptions = arguments.hasArgument(SHOW_TUNABLES_DESCRIPTIONS_FLAG);
        if (withTunables || showDescriptions) {
            String javaVersion = vmInfo.getJavaVersion();
            String message = translator.localize(LocaleResources.GC_PARAMS_MESSAGE,
                    getFormattedParams(commonName, javaVersion, showDescriptions))
                    .getContents();
            out.println(message);
        }
    }
    
    void setVmInfo(VmInfoDAO vmInfoDAO) {
        services.addService(VmInfoDAO.class, vmInfoDAO);
    }
    
    void setVmGcStat(VmGcStatDAO vmGcStat) {
        services.addService(VmGcStatDAO.class, vmGcStat);
    }
    
    void servicesUnavailable() {
        services.removeService(VmInfoDAO.class);
        services.removeService(VmGcStatDAO.class);
    }
    
    /**
     * Checks that a VM record exists in storage and if not throws a
     * command exception with an appropriate message.
     * 
     * @param vmId The VM ID to look up in storage.
     * @param errorMsg The error message to use for when VM is not found.
     * @return A non-null VmInfo.
     * @throws CommandException If the VM could not be found in storage.
     */
    private VmInfo checkVmExists(VmId vmId, LocalizedString errorMsg) throws CommandException {
        VmInfo vmInfo = vmInfoDao.getVmInfo(vmId);
        if (vmInfo == null) {
            throw new CommandException(errorMsg);
        }
        return vmInfo;
    }

    private String getFormattedParams(CollectorCommonName commonName, String javaVersion, boolean showDescriptions) {
        TableRenderer renderer = new TableRenderer(showDescriptions ? 2 : 1);
        if (showDescriptions) {
            renderer.printHeader(translator.localize(LocaleResources.GC_PARAMS_HEADER_FLAG).getContents(),
                    translator.localize(LocaleResources.GC_PARAMS_HEADER_DESC).getContents());
        }
        List<GcParam> params;
        try {
            JavaVersionRange version = JavaVersionRange.fromString(javaVersion);
            params = paramsMapper.getParams(commonName, version);
        } catch (JavaVersionRange.InvalidJavaVersionFormatException | IllegalArgumentException e) {
            logger.warning(translator.localize(LocaleResources.GC_PARAMS_FAILURE_MESSAGE, javaVersion).getContents());
            params = Collections.emptyList();
        }
        for (GcParam param : params) {
            renderer.printLine(getLine(param, showDescriptions));
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        renderer.render(baos);
        return baos.toString();
    }

    private String[] getLine(GcParam param, boolean showDescriptions) {
        if (showDescriptions) {
            return new String[] { param.getFlag(), param.getDescription() };
        } else {
            return new String[] { param.getFlag() };
        }
    }
    
    /**
     * Finds the common name of the GC algorithm used for a given VM.
     * @param vmId The id for the VM in question.
     * @return A common name of the GC algorithm or {@code CollectorCommonName#UNKNOWN_COLLECTOR}.
     */
    private CollectorCommonName getCommonName(VmId vmId) {
        Set<String> distinctCollectors = gcDao.getDistinctCollectorNames(vmId);
        CollectorCommonName commonName = CollectorCommonName.UNKNOWN_COLLECTOR;
        if (distinctCollectors.size() > 0) {
            commonName = commonNameMapper.mapToCommonName(distinctCollectors);
        }
        return commonName;
    }

}
