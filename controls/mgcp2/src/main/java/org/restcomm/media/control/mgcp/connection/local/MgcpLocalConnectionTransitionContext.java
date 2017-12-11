/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.connection.local;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnectionTransitionContext {

    private final Map<MgcpLocalConnectionParameter, Object> data;

    public MgcpLocalConnectionTransitionContext() {
        this.data = new HashMap<>(10);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(MgcpLocalConnectionParameter key, Class<T> type) throws IllegalArgumentException {
        Object value = data.get(key);

        if (value == null) {
            return null;
        } else if (type.isInstance(value)) {
            return (T) value;
        } else {
            throw new IllegalArgumentException("Parameter " + key + "(" + value.getClass().getSimpleName() + ") is not of type " + type);
        }
    }

    public void set(MgcpLocalConnectionParameter key, Object value) {
        this.data.put(key, value);
    }

}
