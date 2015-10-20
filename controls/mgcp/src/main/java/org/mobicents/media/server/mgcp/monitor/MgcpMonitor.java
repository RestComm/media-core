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

package org.mobicents.media.server.mgcp.monitor;

import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.utils.Text;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMonitor implements MgcpConnectionListener {

    private static final Logger LOGGER = Logger.getLogger(MgcpMonitor.class);

    @Override
    public void onConnectionCreated(Map<Text, Text> parameters) {
        // Get all possible parameter
        Text callId = parameters.get(Parameter.CALL_ID);
        Text primaryEndpoint = parameters.get(Parameter.ENDPOINT_ID);
        Text secondaryEndpoint = parameters.get(Parameter.SECOND_ENDPOINT);
        Text primaryConnection = parameters.get(Parameter.CONNECTION_ID);
        Text secondaryConnection = parameters.get(Parameter.CONNECTION_ID2);
        Text mode = parameters.get(Parameter.MODE);
        Text localDescription = parameters.get(Parameter.SDP_OFFER);
        Text remoteDescription = parameters.get(Parameter.SDP_ANSWER);
    }

    @Override
    public void onConnectionModified(Map<Text, Text> parameters) {
        // Get all possible parameter
        Text callId = parameters.get(Parameter.CALL_ID);
        Text connectionId = parameters.get(Parameter.CONNECTION_ID);
        Text mode = parameters.get(Parameter.MODE);
        Text remoteDescription = parameters.get(Parameter.SDP_ANSWER);
    }

    @Override
    public void onConnectionDeleted(Map<Text, Text> parameters) {
        // Get all possible parameter
        Text callId = parameters.get(Parameter.CALL_ID);
        Text connectionId = parameters.get(Parameter.CONNECTION_ID);
        Text endpointId = parameters.get(Parameter.ENDPOINT_ID);

    }

}
