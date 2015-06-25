/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.locale.LocaleResources;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;

public class ApplicationInfo {

    private static final Logger logger = LoggingUtils.getLogger(ApplicationInfo.class);

    private static final String APP_INFO = "/com/redhat/thermostat/app-info.properties";
    
    private Properties appInfo;

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private static final String MISSING_INFO = t.localize(LocaleResources.MISSING_INFO).getContents();
    
    public ApplicationInfo() {
        appInfo = new Properties();
        // the properties file should be in the same package as this class
        InputStream res = getClass().getResourceAsStream(APP_INFO);
        if (res != null) {
            try {
                appInfo.load(res);
            } catch (IOException e) {
                logger.log(Level.WARNING, "error loading application information", e);
            }
        }
    }

    public String getName() {
        return appInfo.getProperty("APP_NAME", MISSING_INFO);
    }

    public Version getVersion() {
        return new Version();
    }

    public String getMavenVersion() {
        return appInfo.getProperty("APP_VERSION");
    }

    public String getDescription() {
        return t.localize(LocaleResources.APPLICATION_INFO_DESCRIPTION).getContents();
    }

    public String getReleaseDate() {
        return appInfo.getProperty("APP_RELEASE_DATE", MISSING_INFO);
    }

    public String getCopyright() {
        return appInfo.getProperty("APP_COPYRIGHT", MISSING_INFO);
    }

    public String getLicenseSummary() {
        return t.localize(LocaleResources.APPLICATION_INFO_LICENSE).getContents();
    }

    public String getEmail() {
        return appInfo.getProperty("APP_EMAIL", MISSING_INFO);
    }

    public String getWebsite() {
        return appInfo.getProperty("APP_WEBSITE", MISSING_INFO);
    }

    public String getUserGuide() {
        return appInfo.getProperty("APP_USER_GUIDE", MISSING_INFO);
    }

    public String getBugReportsAddress() {
        return appInfo.getProperty("BUG_WEBSITE", MISSING_INFO);
    }
}

