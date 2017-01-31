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
    private static final String CONNECTION_ID_SEPARATOR = "@";
    private static final String ACTION_START_SEPARATOR = "(";
    private static final String ACTION_END_SEPARATOR = ")";
    private static final String PARAMETER_START_SEPARATOR = ACTION_START_SEPARATOR;
    private static final String PARAMETER_END_SEPARATOR = ACTION_END_SEPARATOR;

    public static MgcpRequestedEvent[] parse(String requestedEvents, MgcpPackageManager packageManager)
            throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException, UnrecognizedMgcpActionException, MgcpParseException {
        // Split requested events
        String[] tokens = requestedEvents.split("(?<=\\)),");
        MgcpRequestedEvent[] events = new MgcpRequestedEvent[tokens.length];

        // Parse requested events
        for (int i = 0; i < events.length; i++) {
            events[i] = parseSingle(tokens[i], packageManager);
        }
        return events;
    }

    /**
     * AU/oc(N)
     * r/rto@364823(N)(120,st=im)
     * 
     *<package>/<event>[@<connectionId>](<action>)[(<params>)],
     */
    private static MgcpRequestedEvent parseSingle(String requestedEvent, MgcpPackageManager packageManager)
            throws UnrecognizedMgcpPackageException, UnrecognizedMgcpEventException, UnrecognizedMgcpActionException, MgcpParseException {
        // Get indexes of separators
        int indexOfEvent = requestedEvent.indexOf(EVENT_SEPARATOR);
        if (indexOfEvent == -1) {
            throw new UnrecognizedMgcpPackageException("Missing event name separator on " + requestedEvent);
        }
        
        int indexOfConnectionId = requestedEvent.indexOf(CONNECTION_ID_SEPARATOR);
        
        int indexOfActionStart = requestedEvent.indexOf(ACTION_START_SEPARATOR);
        if (indexOfActionStart == -1) {
            throw new MgcpParseException("Missing action start separator on " + requestedEvent);
        }

        int indexOfActionEnd = requestedEvent.indexOf(ACTION_END_SEPARATOR);
        if (indexOfActionEnd == -1) {
            throw new MgcpParseException("Missing action end separator on " + requestedEvent);
        }
        
        // Parse information
        try {
            // Load MGCP Package
            String packageName = requestedEvent.substring(0, indexOfEvent);
            MgcpPackage mgcpPackage = packageManager.getPackage(packageName);
            
            if (mgcpPackage == null) {
                throw new UnrecognizedMgcpPackageException("Unrecognized package " + packageName + " on " + requestedEvent);
            }

            // Load MGCP Event
            String eventName;
            if(indexOfConnectionId == -1) {
                eventName = requestedEvent.substring(indexOfEvent + 1, indexOfActionStart);
            } else {
                eventName = requestedEvent.substring(indexOfEvent + 1, indexOfConnectionId);
            }
            MgcpEventType eventDetails = mgcpPackage.getEventDetails(eventName);
            
            if (eventDetails == null) {
                throw new UnrecognizedMgcpEventException("Unrecognized event " + eventName + " on " + requestedEvent);
            }
            
            String connectionIdHex = "";
            if(indexOfConnectionId != -1) {
                connectionIdHex = requestedEvent.substring(indexOfConnectionId + 1, indexOfActionStart);
                if(!connectionIdHex.matches("^[0-9A-F]+$")) {
                    throw new MgcpParseException("Invalid connection identifier " + connectionIdHex + " on " + requestedEvent);
                }
            }

            String eventParameters = "";
            boolean parameterized = eventDetails.parameterized();
            if (parameterized) {
                int indexOfParameterStart = requestedEvent.indexOf(PARAMETER_START_SEPARATOR, indexOfActionEnd);
                if (indexOfParameterStart == -1) {
                    throw new MgcpParseException("Missing parameters start separator on " + requestedEvent);
                }

                int indexOfParameterEnd = requestedEvent.indexOf(PARAMETER_END_SEPARATOR, indexOfParameterStart);
                if (indexOfActionEnd == -1) {
                    throw new MgcpParseException("Missing parameters end separator on " + requestedEvent);
                }
                
                eventParameters = requestedEvent.substring(indexOfParameterStart + 1, indexOfParameterEnd);
            }

            // Parse Action
            String action = requestedEvent.substring(indexOfActionStart + 1, indexOfActionEnd);
            MgcpActionType actionType = MgcpActionType.fromSymbol(action);
            if (actionType == null) {
                throw new UnrecognizedMgcpActionException("Unrecognized action " + eventName + " on " + requestedEvent);
            }

            // Build object
            int connectionId = connectionIdHex.isEmpty() ? 0 : Integer.parseInt(connectionIdHex, 16);
            String[] eventParametersTokens = eventParameters.isEmpty() ? new String[0] : eventParameters.split(",");
            return new MgcpRequestedEvent(packageName, eventName, actionType, connectionId, eventParametersTokens);
        } catch (RuntimeException e) {
            throw new MgcpParseException("Could not parse requested event " + requestedEvent, e);
        }
    }

}
