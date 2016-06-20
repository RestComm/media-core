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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpSignal implements MgcpSignal {

    private final String packageName;
    private final String symbol;
    private final SignalType type;
    private final Map<String, String> parameters;
    private String[] requestedEvents;

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type) {
        super();
        this.packageName = packageName;
        this.symbol = symbol;
        this.type = type;
        this.parameters = new HashMap<>(10);
    }

    public String getSymbol() {
        return symbol;
    }

    public SignalType getType() {
        return type;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    public void addParameter(String name, String value) throws IllegalArgumentException {
        if (!isParameterSupported(name)) {
            throw new IllegalArgumentException("Parameter " + name + " is not supported by signal " + this.symbol);
        }
        this.parameters.put(name, value);
    }

    protected abstract boolean isParameterSupported(String name);
    
    @Override
    public void listen(String... events) throws IllegalArgumentException {
        // validate event list
        for (String event : events) {
            if (!isEventSupported(event)) {
                throw new IllegalArgumentException("Event " + event + " is not supported by signal " + this.symbol);
            }
        }
        // Register event list
        this.requestedEvents = events;
    }

    protected abstract boolean isEventSupported(String name);
    
}
