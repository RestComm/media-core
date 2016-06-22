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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @category NotThreadSafe
 */
public class GenericMgcpEvent implements MgcpEvent {

    private final String symbol;
    private final Map<String, String> parameters;
    private final AtomicBoolean fired;

    public GenericMgcpEvent(String symbol) {
        super();
        this.symbol = symbol;
        this.parameters = new HashMap<>(10);
        this.fired = new AtomicBoolean(false);
    }

    public String getSymbol() {
        return this.symbol;
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }
    
    public void setParameter(String name, String value) {
        this.parameters.put(name, value);
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

}
