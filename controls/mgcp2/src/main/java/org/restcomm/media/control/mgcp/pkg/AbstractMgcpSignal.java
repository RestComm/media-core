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

package org.restcomm.media.control.mgcp.pkg;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;

/**
 * Generic representation of an MGCP Signal.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @deprecated use org.restcomm.media.control.mgcp.signal.AbstractSignal
 */
@Deprecated
public abstract class AbstractMgcpSignal implements MgcpSignal {

    private final int requestId;
    private final String packageName;
    private final String symbol;
    private final SignalType type;
    private final NotifiedEntity notifiedEntity;
    private final Map<String, String> parameters;
    private final List<MgcpEventObserver> observers;
    protected final AtomicBoolean executing;

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type, int requestId, NotifiedEntity notifiedEntity, Map<String, String> parameters) {
        super();
        this.packageName = packageName;
        this.symbol = symbol;
        this.type = type;
        this.requestId = requestId;
        this.parameters = parameters;
        this.observers = new CopyOnWriteArrayList<>();
        this.executing = new AtomicBoolean(false);
        this.notifiedEntity = notifiedEntity;
    }

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type, int requestId, Map<String, String> parameters) {
        this(packageName, symbol, type, requestId, null, parameters);
    }

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type, int requestId, NotifiedEntity notifiedEntity) {
        this(packageName, symbol, type, requestId, notifiedEntity, Collections.<String, String> emptyMap());
    }

    public AbstractMgcpSignal(String packageName, String symbol, SignalType type, int requestId) {
        this(packageName, symbol, type, requestId, null, Collections.<String, String> emptyMap());
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getName() {
        return this.packageName + "/" + this.symbol;
    }

    @Override
    public SignalType getSignalType() {
        return type;
    }

    @Override
    public int getRequestId() {
        return this.requestId;
    }
    
    @Override
    public NotifiedEntity getNotifiedEntity() {
        return this.notifiedEntity;
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
    public void observe(MgcpEventObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void forget(MgcpEventObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notify(Object originator, MgcpEvent event) {
        Iterator<MgcpEventObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpEventObserver observer = iterator.next();
            if (observer != originator) {
                observer.onEvent(originator, event);
            }
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

    @Override
    public String toString() {
        return this.packageName + "/" + this.symbol;
    }

}
