/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.plugin.validator.internal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.redhat.thermostat.plugin.validator.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ConfigurationValidatorErrorHandler implements ErrorHandler {

    private static final Logger logger = LoggingUtils.getLogger(ConfigurationValidatorErrorHandler.class);
    private int warningsErrorsCounter = 0;
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private enum ErrorType {
        WARNING,
        ERROR,
        FATAL_ERROR;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        warningsErrorsCounter++;
        printInfo(exception, ErrorType.WARNING);
    }

    @Override
    public void error(SAXParseException exception) throws SAXParseException {
        warningsErrorsCounter++;
        printInfo(exception, ErrorType.ERROR);
    }
    
    @Override
    public void fatalError(SAXParseException exception) throws SAXParseException {
        if (warningsErrorsCounter == 0) {
            printInfo(exception, ErrorType.FATAL_ERROR);
            logger.warning("XML not well formed");
        }
    }

    private static void printInfo(SAXParseException e, ErrorType type) {
        int columnNumber = e.getColumnNumber();
        int lineNumber = e.getLineNumber();
        
        StringBuffer buffer = new StringBuffer();
        
        String firstLine = null;
        String secondLine = null;
        String thirdLine = null;
        String errorLine = null;
        String pointer = "";
        String absolutePath = e.getSystemId();
        absolutePath = absolutePath.substring(5);
        
        Map<ErrorType,LocaleResources> translateKeys = new HashMap<>();
        translateKeys.put(ErrorType.ERROR, LocaleResources.VALIDATION_ERROR);
        translateKeys.put(ErrorType.WARNING, LocaleResources.VALIDATION_WARNING);
        translateKeys.put(ErrorType.FATAL_ERROR, LocaleResources.VALIDATION_FATAL_ERROR);
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(absolutePath));
            for (int i = 1; i < lineNumber-3; i++) {
                br.readLine();
            }
            firstLine = br.readLine();
            secondLine = br.readLine();
            thirdLine = br.readLine();
            errorLine = br.readLine();
            
            for (int j = 1; j < columnNumber-1; j++) {
                pointer = pointer.concat(" ");
            }
            pointer = pointer.concat("^");
            br.close();
        } catch (IOException exception) {
            System.out.println("File not found!");;
        }
        
        buffer.append(translator.localize(
                translateKeys.get(type),
                absolutePath, 
                Integer.toString(lineNumber), 
                Integer.toString(columnNumber)).getContents());
                    
        buffer.append(formatMessage(e.getLocalizedMessage()) + "\n\n");
        buffer.append(firstLine + "\n");
        buffer.append(secondLine + "\n");
        buffer.append(thirdLine + "\n");
        buffer.append(errorLine + "\n");
        buffer.append(pointer  + "\n");
        
        logger.warning("\n" + buffer.toString());
    }
    
    private static String formatMessage(String message) {
        String[] arguments = message.split("\"http://icedtea.classpath.org/thermostat/plugins/v1.0\":");
        int size = arguments.length;
        String output = "";
        for (int i = 0; i < size; i++) {
            output=output.concat(arguments[i]);
        }
        return output;
    }
}
