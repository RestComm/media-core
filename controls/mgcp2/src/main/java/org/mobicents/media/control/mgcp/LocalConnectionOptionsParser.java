/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.control.mgcp;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;

/**
 * Parses Local Connection Options of an MGCP message.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class LocalConnectionOptionsParser {

    private static final Logger log = Logger.getLogger(LocalConnectionOptionsParser.class);

    public LocalConnectionOptions parse(String options) throws MgcpParseException {
        // Create new object to persist options to
        LocalConnectionOptions obj = new LocalConnectionOptions();

        try {
            // Get all options
            String[] tokens = options.split(",");
            for (String token : tokens) {
                token = token.trim();
                int separator = token.indexOf(":");
                if (separator == -1) {
                    // Add option with no value
                    LocalConnectionOptionType option = LocalConnectionOptionType.fromCode(token);
                    obj.add(option, "");
                } else {
                    // Add option with value
                    String code = token.substring(0, separator);
                    try {
                        LocalConnectionOptionType option = LocalConnectionOptionType.fromCode(code);
                        String value = token.substring(separator + 1);
                        obj.add(option, value);
                    } catch (IllegalArgumentException e) {
                        log.warn("Skipping unknown option: " + code);
                    }
                }
            }
            return obj;
        } catch (Exception e) {
            throw new MgcpParseException("Could not parse Local Connection Options.", e);
        }
    }

}
