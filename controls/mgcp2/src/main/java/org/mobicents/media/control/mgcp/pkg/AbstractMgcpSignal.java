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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic representation of an MGCP Signal.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpSignal implements MgcpSignal {

    private final String packageName;
    private final String symbol;
    private final SignalType type;
    private final Map<String, String> parameters;
    private final List<MgcpEventListener> observers;
    protected final AtomicBoolean executing;

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type, Map<String, String> parameters) {
        super();
        this.packageName = packageName;
        this.symbol = symbol;
        this.type = type;
        this.parameters = parameters;
        this.observers = new CopyOnWriteArrayList<>();
        this.executing = new AtomicBoolean(false);
    }

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type) {
        this(packageName, symbol, type, Collections.<String, String> emptyMap());
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

    protected abstract boolean isParameterSupported(String name);

    @Override
    public boolean isExecuting() {
        return this.executing.get();
    }

    @Override
    public void observe(MgcpEventListener observer) {
        this.observers.add(observer);
    }

    @Override
    public void forget(MgcpEventListener observer) {
        this.observers.remove(observer);
    }

    protected void fire(MgcpEvent event) {
        Iterator<MgcpEventListener> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpEventListener listener = iterator.next();
            listener.onMgcpEvent(event);
        }
    }

    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj != null && obj instanceof AbstractMgcpSignal) {
            AbstractMgcpSignal other = (AbstractMgcpSignal) obj;
            equals = this.packageName.equalsIgnoreCase(other.packageName) && this.symbol.equalsIgnoreCase(other.symbol)
                    && this.type.equals(other.type) && this.parameters.size() == other.parameters.size();

            if (equals) {
                for (String key : this.parameters.keySet()) {
                    if (!this.parameters.get(key).equals(other.parameters.get(key))) {
                        equals = false;
                        break;
                    }
                }
            }
        }
        return equals;
    }

}
