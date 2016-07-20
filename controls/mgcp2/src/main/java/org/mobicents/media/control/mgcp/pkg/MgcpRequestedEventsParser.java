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

package org.mobicents.media.control.mgcp.pkg;

import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.pkg.au.AudioPackage;
import org.mobicents.media.control.mgcp.pkg.base.MgcpEventType;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpActionException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpEventException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;

/**
 * Parses RequestedEvent parameter for an MGCP RQNT command.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequestedEventsParser {

    private static final String EVENT_SEPARATOR = "/";
    private static final String ACTION_START_SEPARATOR = "(";
    private static final String ACTION_END_SEPARATOR = ")";

    public static MgcpRequestedEvent[] parse(String requestedEvents)
            throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException, UnrecognizedMgcpActionException, MgcpParseException {
        // Split requested events
        String[] tokens = requestedEvents.split(",");
        MgcpRequestedEvent[] events = new MgcpRequestedEvent[tokens.length];

        // Parse requested events
        for (int i = 0; i < events.length; i++) {
            events[i] = parseSingle(tokens[i]);
        }
        return events;
    }

    private static MgcpRequestedEvent parseSingle(String requestedEvent)
            throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException, UnrecognizedMgcpActionException, MgcpParseException {
        // Get indexes of separators
        int indexOfEvent = requestedEvent.indexOf(EVENT_SEPARATOR);
        if (indexOfEvent == -1) {
            throw new UnrecognizedMgcpPackageException("Missing event name separator");
        }

        int indexOfActionStart = requestedEvent.indexOf(ACTION_START_SEPARATOR);
        if (indexOfActionStart == -1) {
            throw new MgcpParseException("Missing action start separator");
        }

        int indexOfActionEnd = requestedEvent.indexOf(ACTION_END_SEPARATOR);
        if (indexOfActionEnd == -1) {
            throw new MgcpParseException("Missing action end separator");
        }

        try {
            // Extract information
            String packageName = requestedEvent.substring(0, indexOfEvent);
            if (!validatePackage(packageName)) {
                throw new UnrecognizedMgcpPackageException("Unrecognized package: " + packageName);
            }

            String eventName = requestedEvent.substring(indexOfEvent + 1, indexOfActionStart);
            if (!validateEvent(eventName)) {
                throw new UnrecognizedMgcpEventException("Unrecognized event: " + eventName);
            }

            String action = requestedEvent.substring(indexOfActionStart + 1, indexOfActionEnd);
            MgcpActionType actionType = MgcpActionType.fromSymbol(action);
            if (actionType == null) {
                throw new UnrecognizedMgcpActionException("Unrecognized action: " + eventName);
            }

            // Build object
            return new MgcpRequestedEvent(packageName, eventName, actionType);
        } catch (RuntimeException e) {
            throw new MgcpParseException("Could not parse requested event.", e);
        }
    }

    private static boolean validatePackage(String name) {
        switch (name) {
            case AudioPackage.PACKAGE_NAME:
                return true;

            default:
                return false;
        }
    }

    private static boolean validateEvent(String name) {
        return (MgcpEventType.fromSymbol(name) != null);
    }

}
