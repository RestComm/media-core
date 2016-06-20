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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AbstractMgcpEvent implements MgcpEvent {

    private final String symbol;
    private final Map<String, String> parameters;
    private final List<MgcpEventListener> eventListeners;
    private final AtomicBoolean fired;

    public AbstractMgcpEvent(String symbol) {
        super();
        this.symbol = symbol;
        this.parameters = new HashMap<>(10);
        this.eventListeners = new ArrayList<>(5);
        this.fired = new AtomicBoolean(false);
    }

    @Override
    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    @Override
    public void fire() {
        if (this.fired.get()) {
            throw new IllegalStateException("Event was already fired");
        }

        // Broadcast event across all listeners
        this.fired.set(true);
        for (MgcpEventListener listener : this.eventListeners) {
            listener.onMgcpEvent(this);
        }

        // Clean event state (since it cannot be fired twice)
        clean();
    }

    private void clean() {
        this.parameters.clear();
        this.eventListeners.clear();
    }

}
