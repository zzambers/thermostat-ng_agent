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

package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.testutils.AbstractLocaleResourcesTest;

public class LocaleResourcesTest extends AbstractLocaleResourcesTest<LocaleResources> {

    @Override
    protected Class<LocaleResources> getEnumClass() {
        return LocaleResources.class;
    }

    @Override
    protected String getResourceBundle() {
        return LocaleResources.RESOURCE_BUNDLE;
    }
    
    /*
     * MessageFormat[1] treats single quotes as an escape character.
     * 
     * Having something like -->'{0}'<-- in strings.properties makes the
     * pattern look like a literal {0} to MessageFormat. That is, the parameter
     * does not get substituted. The correct pattern is ''{0}''.
     * 
     * These tests make sure params get properly substituted and don't show
     * up wrongly at runtime.
     * 
     * [1] http://docs.oracle.com/javase/7/docs/api/java/text/MessageFormat.html
     */
    @Test
    public void canBindVariables() {
        Translate<LocaleResources> t = LocaleResources.createLocalizer();
        doStringSubstitutionTest(t, LocaleResources.MONGODB_SETUP_FILE_EXISTS);
        doStringSubstitutionTest(t, LocaleResources.STAMP_FILE_CREATION_FAILED);
        doStringSubstitutionTest(t, LocaleResources.UNKNOWN_STORAGE_URL);
        doStringSubstitutionTest(t, LocaleResources.USERNAME_PROMPT);
    }
    
    private void doStringSubstitutionTest(Translate<LocaleResources> t, LocaleResources res) {
        String filename = "foobar";
        LocalizedString string = t.localize(res, filename);
        assertTrue("String '" + string.getContents() + "' should have contained '" + filename + "'", string.getContents().contains(filename));
    }

}

