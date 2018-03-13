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

package org.restcomm.media.core.control.mgcp.pkg;

import java.text.ParseException;

import org.restcomm.media.core.control.mgcp.command.param.NotifiedEntity;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifiedEntityParser {

    private static final String DOMAIN_SEPARATOR = "@";
    private static final String PORT_SEPARATOR = ":";

    public static NotifiedEntity parse(String notifiedEntity) throws ParseException {
        // Local Name
        int indexOfDomain = notifiedEntity.indexOf(DOMAIN_SEPARATOR);
        if (indexOfDomain == -1) {
            throw new ParseException("Missing domain: " + notifiedEntity, indexOfDomain);
        }

        String localName = notifiedEntity.substring(0, indexOfDomain);
        if (localName.isEmpty()) {
            throw new ParseException("Missing local name: " + notifiedEntity, 0);
        }

        // Domain Name
        int indexOfPort = notifiedEntity.indexOf(PORT_SEPARATOR);
        String domainName;
        if (indexOfPort == -1) {
            domainName = notifiedEntity.substring(indexOfDomain + 1);
        } else {
            domainName = notifiedEntity.substring(indexOfDomain + 1, indexOfPort);
        }

        if (domainName.isEmpty()) {
            throw new ParseException("Missing domain name: " + notifiedEntity, indexOfDomain);
        }

        if (indexOfPort > -1 && indexOfPort + 1 == notifiedEntity.length()) {
            throw new ParseException("Missing port: " + notifiedEntity, indexOfDomain);
        }

        // Port
        int port = 0;
        if (indexOfPort > -1) {
            try {
                port = Integer.parseInt(notifiedEntity.substring(indexOfPort + 1));
            } catch (NumberFormatException e) {
                throw new ParseException("Wrong port format: " + notifiedEntity, indexOfDomain);
            }
        }

        // Build Notified Entity
        return new NotifiedEntity(localName, domainName, port);
    }

}
