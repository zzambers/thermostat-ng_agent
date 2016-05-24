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

package com.redhat.thermostat.common.cli;

import com.redhat.thermostat.annotations.Service;

/**
 * A stub interface for marking a TabCompleter which specifically provides file name completions.
 * This is intended to be exported as an OSGi service. If you are writing a {@link CompleterService}
 * which provides file name completion, you may use this interface as a dependency and use the
 * Thermostat-provided file name completions this way.
 *
 * However, if it is applicable, you may alternatively simply use the convention of naming your
 * filename option argument -f/--filename (ex. foo-command -f /path/to/file, or
 * foo-command --filename /path/to/file). If you define your file name completion argument to match
 * this naming convention exactly in your thermostat-plugin.xml, then the filename completions will
 * be taken care of automatically by Thermostat.
 */
@Service
public interface FileNameTabCompleter extends TabCompleter {
}
