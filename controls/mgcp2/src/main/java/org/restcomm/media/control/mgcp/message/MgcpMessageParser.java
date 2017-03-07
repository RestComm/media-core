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

package org.restcomm.media.control.mgcp.message;

import org.restcomm.media.control.mgcp.exception.MgcpParseException;

/**
 * Parses text into MGCP Message objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMessageParser {

    private final LocalConnectionOptionsParser optionsParser;
    
    public MgcpMessageParser() {
        this.optionsParser = new LocalConnectionOptionsParser();
    }

    public MgcpRequest parseRequest(byte[] data, int offset, int length) throws MgcpParseException {
        return parseRequest(new String(data, offset, length));
    }

    public MgcpRequest parseRequest(String message) throws MgcpParseException {
        try {
            MgcpRequest request = new MgcpRequest();
            parseMgcpRequest(message, request);
            return request;
        } catch (Exception e) {
            throw new MgcpParseException("Could not parse MGCP request.", e);
        }
    }

    private void parseMgcpRequest(String message, MgcpRequest request) throws Exception {
        String[] lines = message.split(System.lineSeparator());

        // Analyze request header
        String header = lines[0];
        String[] headerParams = header.split(" ");

        // Set Request type
        String command = headerParams[0];
        MgcpRequestType requestType = MgcpRequestType.valueOf(command.toUpperCase());
        request.setRequestType(requestType);

        // Set transaction ID
        String transactionId = headerParams[1];
        request.setTransactionId(Integer.parseInt(transactionId));

        // Set endpoint ID
        String endpointId = headerParams[2];
        request.setEndpointId(endpointId);

        // Set parameters and SDP
        parseParametersAndSdp(lines, request);
        
        // Parse Local Connection Options (if present)
        String lcOptions = request.getParameter(MgcpParameterType.LOCAL_CONNECTION_OPTIONS);
        if(lcOptions != null) {
            this.optionsParser.parse(lcOptions);
        }
    }

    public MgcpResponse parseResponse(byte[] data, int offset, int length) throws MgcpParseException {
        return parseResponse(new String(data, offset, length));
    }

    public MgcpResponse parseResponse(String message) throws MgcpParseException {
        MgcpResponse response = new MgcpResponse();
        try {
            parseResponse(message, response);
        } catch (Exception e) {
            throw new MgcpParseException("Could not parse MGCP response", e);
        }
        return response;
    }

    private void parseResponse(String message, MgcpResponse response) throws Exception {
        String[] lines = message.split(System.lineSeparator());

        // Analyze request header
        String header = lines[0];

        // Set Request type
        int codeSeparator = header.indexOf(" ");
        String returnCode = header.substring(0, codeSeparator);
        response.setCode(Integer.parseInt(returnCode));

        // Set transaction ID
        int transactionIdSeparator = header.indexOf(" ", codeSeparator + 1);
        String transactionId = header.substring(codeSeparator + 1, transactionIdSeparator);
        response.setTransactionId(Integer.parseInt(transactionId));

        // Set endpoint ID
        String returnMessage = header.substring(transactionIdSeparator + 1);
        response.setMessage(returnMessage);

        // Set parameters and SDP
        parseParametersAndSdp(lines, response);
    }

    private void parseParametersAndSdp(String[] lines, MgcpMessage message) throws Exception {
        // Get MGCP parameters and SDP
        StringBuilder sdpBuilder = new StringBuilder();
        boolean sdp = false;
        int nLines = lines.length;
        for (int i = 1; i < nLines; i++) {
            String line = lines[i];

            if (sdp) {
                // Build SDP description
                sdpBuilder.append(line);
                if (i < nLines - 1) {
                    sdpBuilder.append(System.lineSeparator());
                }
            } else {
                if (line.isEmpty()) {
                    // Set SDP detected
                    sdp = true;
                } else {
                    // Add parameter
                    int separatorIndex = line.indexOf(":");
                    MgcpParameterType type = MgcpParameterType.fromCode(line.substring(0, separatorIndex));
                    message.addParameter(type, line.substring(separatorIndex + 1));
                }
            }
        }

        // Set SDP (if present)
        if (sdp) {
            message.addParameter(MgcpParameterType.SDP, sdpBuilder.toString());
        }
    }

}
