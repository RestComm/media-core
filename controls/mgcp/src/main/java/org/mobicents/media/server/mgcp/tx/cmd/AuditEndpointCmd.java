/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp.tx.cmd;

import jain.protocol.ip.mgcp.message.parms.ReasonCode;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.mgcp.controller.NotifiedEntity;
import org.mobicents.media.server.mgcp.controller.naming.UnknownEndpointException;
import org.mobicents.media.server.mgcp.controller.signal.RequestedEvent;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.mgcp.pkg.sl.SignalRequest;
import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.utils.Text;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @see <a href="https://tools.ietf.org/html/rfc3435#section-2.3.10">RFC 3435</a>
 */
public class AuditEndpointCmd extends Action {

    private static final Logger LOGGER = Logger.getLogger(AuditConnectionCmd.class);

    // Core elements
    private final Scheduler scheduler;

    // MGCP elements
    private MgcpRequest request;
    private Parameter endpointId;
    private Parameter requestedInfo;

    private Text localName = new Text("");
    private Text domainName = new Text("");
    private Text[] endpointName = new Text[] { localName, domainName };

    // Audited information
    private static final int DEFAULT_ENDPOINT_LIMIT = 100;
    private MgcpEndpoint[] endpoints = new MgcpEndpoint[DEFAULT_ENDPOINT_LIMIT];
    
    private RequestedEvent[] requestedEvents;
//    private DigitMap digitMap;
    private SignalRequest[] requestedSignals;
    private RequestIdentifier requestId;
//    private QuarantineHandling quarantineHandling;
    private NotifiedEntity notifiedEntity;
    private Text primaryConnectionId;
    private Text secondaryConnectionId;
//    Parameter.DETECT_EVENTS xxxxx
//    Parameter.OBSERVED_EVENT
//    Parameter.EVENT_STATES xxxxx
    private Text bearerInformation;
//    Parameter.RESTART_METHOD xxxxx
//    Parameter.RESTART_DELAY xxxxx
    private ReasonCode reasonCode;
    private Package
    Parameter.PACKAGE_LIST xxxxx
    Parameter.MAX_MGCP_DATAGRAM xxxxx
    Parameter.CAPABILITIES xxxxx
    
    // MGCP wildcards
    private final static char ANY = '$';
    private final static char ALL = '*';

    // MGCP error messages
    private final static Text ENDPOINT_ID_MISSING = new Text("Missing endpoint identifier");
    private final static Text ENDPOINT_ID_ANY = new Text("Cannot use wildcard * as endpoint indentifier");
    private final static Text ENDPOINT_INEXISTENT = new Text("Endpoint not available");
    private final static Text ENDPOINT_LIST_TOO_LARGE = new Text("Endpoint list is too large");

    public AuditEndpointCmd(final Scheduler scheduler) {
        // Core elements
        this.scheduler = scheduler;
    }

    private class Audit extends Task {

        @Override
        public int getQueueNumber() {
            return Scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            // Extract parameters from MGCP request
            request = (MgcpRequest) getEvent().getMessage();
            endpointId = request.getParameter(Parameter.ENDPOINT_ID);
            requestedInfo = request.getParameter(Parameter.REQUESTED_INFO);
            
            // Validate the parameters
            if (request.getEndpoint() == null || request.getEndpoint().length() == 0) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, ENDPOINT_ID_MISSING);
            } else {
                request.getEndpoint().divide('@', endpointName);
                if(localName.contains(ANY)) {
                    throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, ENDPOINT_ID_ANY);
                }
            }

            // Search for the MGCP endpoint
            int endpointCount = findMgcpEndpoints(localName, endpoints);
            
            if(localName.contains(ALL)) {
                // TODO Ignore requested information and just retrieve the list of Endpoint IDs
                
            } else {
                // Retrieve requested information from the endpoint
                MgcpEndpoint endpoint = endpoints[0];
                if(requestedInfo != null) {
                    Collection<Text> requestedParams = requestedInfo.getValue().split(',');
                    auditRequestedInfo(requestedParams, endpoint);
                }
            }


            return 0;
        }

        private int findMgcpEndpoints(final Text localName, final MgcpEndpoint[] endpoints) {
            try {
                int n = transaction().find(localName, endpoints);
                if (n == 0) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ENDPOINT_INEXISTENT);
                } else if (n > DEFAULT_ENDPOINT_LIMIT) {
                    throw new MgcpCommandException(MgcpResponseCode.RESPONSE_TOO_LARGE, ENDPOINT_LIST_TOO_LARGE);
                }
                return n;
            } catch (UnknownEndpointException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, ENDPOINT_INEXISTENT);
            }
        }
        
        private void auditRequestedInfo(Collection<Text> parameters, MgcpEndpoint endpoint) {
            for (Text parameter : parameters) {
                Parameter.REQUESTED_EVENTS;
                Parameter.DIGIT_MAP xxxxx
                Parameter.REQUESTED_SIGNALS
                Parameter.REQUEST_ID
                Parameter.QUARANTINE_HANDLING xxxxx
                Parameter.NOTIFIED_ENTITY
                Parameter.CONNECTION_ID
                Parameter.CONNECTION_ID2
                Parameter.DETECT_EVENTS xxxxx
                Parameter.OBSERVED_EVENT
                Parameter.EVENT_STATES xxxxx
                Parameter.BARER_INFORMATION
                Parameter.RESTART_METHOD xxxxx
                Parameter.RESTART_DELAY xxxxx
                Parameter.REASON_CODE
                Parameter.PACKAGE_LIST xxxxx
                Parameter.MAX_MGCP_DATAGRAM xxxxx
                Parameter.CAPABILITIES xxxxx
            }
            
        }

    }

}
