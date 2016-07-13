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

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SignalsRequestParser {

    private static final String NAMESPACE_SEPARATOR = "/";
    private static final String START_PARAM_SEPARATOR = "(";
    private static final String END_PARAM_SEPARATOR = ")";
    private static final String PARAM_SEPARATOR = " ";
    private static final String VALUE_SEPARATOR = "=";

    public static SignalRequests parse(String request) throws MgcpParseException {
        try {
            // Get index of separators
            int indexOfNamespace = request.indexOf(NAMESPACE_SEPARATOR);
            int indexOfStartParam = request.indexOf(START_PARAM_SEPARATOR);
            int indexOfEndParam = request.indexOf(END_PARAM_SEPARATOR);

            if (indexOfNamespace == -1 || indexOfStartParam == -1 || indexOfEndParam == -1) {
                throw new IllegalArgumentException("Missing separator");
            }

            // Break request String
            String packageName = request.substring(0, indexOfNamespace);
            String signalType = request.substring(indexOfNamespace + 1, indexOfStartParam);
            String[] parameters = request.substring(indexOfStartParam + 1, indexOfEndParam).split(PARAM_SEPARATOR);

            if (parameters.length == 0) {
                throw new IllegalArgumentException("Signal Request parameters missing.");
            }

            // Create Object
            SignalRequests obj = new SignalRequests(packageName, signalType);
            for (String param : parameters) {
                int indexOfValue = param.indexOf(VALUE_SEPARATOR);
                if (indexOfValue == -1) {
                    throw new IllegalArgumentException("Value is missing separator.");
                }

                String key = param.substring(0, indexOfValue);
                String value = param.substring(indexOfValue + 1);
                obj.addParameter(key, value);
            }
            return obj;
        } catch (Exception e) {
            throw new MgcpParseException("Malformed signal request: " + request, e);
        }

    }

}
