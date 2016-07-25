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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mobicents.media.control.mgcp.message.MgcpParameterType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class GenericMgcpEvent implements MgcpEvent {

    private final String pkg;
    private final String symbol;
    private final String signal;
    private final Map<String, String> parameters;
    private final AtomicBoolean fired;
    private final StringBuilder builder;

    public GenericMgcpEvent(String pkg, String symbol, String signal) {
        this.pkg = pkg;
        this.symbol = symbol;
        this.signal = signal;
        this.parameters = new HashMap<>(5);
        this.fired = new AtomicBoolean(false);
        this.builder = new StringBuilder();
    }

    @Override
    public String getPackage() {
        return this.pkg;
    }

    @Override
    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public String getSignal() {
        return this.signal;
    }

    @Override
    public String getParameter(MgcpParameterType type) {
        return this.parameters.get(type);
    }

    public void setParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    @Override
    public void fire(MgcpEventListener... targets) {
        if (this.fired.get()) {
            throw new IllegalStateException("Event was already fired");
        }

        // Broadcast event across all listeners
        this.fired.set(true);
        for (MgcpEventListener target : targets) {
            target.onMgcpEvent(this);
        }

        // Clean event state (since it cannot be fired twice)
        clean();
    }

    private void clean() {
        this.parameters.clear();
    }

    @Override
    public String toString() {
        this.builder.setLength(0);
        this.builder.append(this.pkg).append("/").append(this.symbol).append("(");
        this.builder.append(this.pkg).append("/").append(this.signal);

        if (!this.parameters.isEmpty()) {
            Iterator<String> iterator = this.parameters.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = this.parameters.get(key);

                this.builder.append(key);
                if (value != null) {
                    this.builder.append("=").append(value);
                }

                if (iterator.hasNext()) {
                    this.builder.append(" ");
                }
            }
        }
        this.builder.append(")");
        return builder.toString();
    }

}
