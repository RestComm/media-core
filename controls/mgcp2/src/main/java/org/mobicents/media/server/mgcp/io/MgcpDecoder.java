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

package org.mobicents.media.server.mgcp.io;

import org.mobicents.media.server.mgcp.MgcpActionType;
import org.mobicents.media.server.mgcp.MgcpParameterType;
import org.mobicents.media.server.mgcp.MgcpRequest;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpDecoder {

    public static MgcpRequest parseRequest(String message) {
        MgcpRequest request = new MgcpRequest();

        // Split the message into lines
        String[] lines = message.split(System.lineSeparator());

        // Parse the command header
        String command = lines[0];
        String[] commandTokens = command.split(" ");

        MgcpActionType actionType = MgcpActionType.valueOf(commandTokens[0]);
        int transactionId = Integer.valueOf(commandTokens[1]);
        String endpointId = commandTokens[2].substring(0, commandTokens[2].indexOf('@'));

        request.setActionType(actionType);
        request.setEndpointId(endpointId);
        request.setTransactionId(transactionId);

        // Parse the parameters and lookup for possible SDP
        int sdpIndex = -1;
        int linesLength = lines.length;

        for (int i = 1; i < linesLength; i++) {
            int splitterIndex = lines[i].indexOf(':');
            if (splitterIndex != -1) {
                // Check the parameter is valid
                String paramName = lines[i].substring(0, splitterIndex);
                MgcpParameterType parameterType = MgcpParameterType.fromCode(paramName);

                if (parameterType == null) {
                    throw new IllegalArgumentException("Unknow MGCP Parameter: " + paramName);
                }

                // Add the parameter to the request
                request.setParameter(parameterType, lines[i].substring(splitterIndex + 1).trim());
            } else {
                // The line belongs to sdp. Stop parsing parameters.
                sdpIndex = i;
                break;
            }
        }

        // Parse SDP (if exists)
        if (sdpIndex > -1) {
            StringBuilder builder = new StringBuilder();
            for (int index = sdpIndex; index < linesLength; index++) {
                builder.append(lines[index]).append("\n");
            }
            request.setSdp(builder.toString().trim());
        }

        return request;
    }

}
