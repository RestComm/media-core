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

package org.restcomm.media.control.mgcp.endpoint.provider;

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;

/**
 * Provides MGCP endpoints for a specific name space.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpEndpointProvider<T extends MgcpEndpoint> implements MgcpEndpointProvider<T> {
    
    private final String namespace;
    private final String domain;
    private final AtomicInteger idGenerator;
    
    public AbstractMgcpEndpointProvider(String namespace, String domain) {
        this.namespace = namespace;
        this.domain = domain;
        this.idGenerator = new AtomicInteger(0);
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }
    
    @Override
    public String getDomain() {
        return this.domain;
    }
    
    protected String generateId() {
        return this.namespace + nextId();
    }
    
    private int nextId() {
        this.idGenerator.compareAndSet(Integer.MAX_VALUE, 0);
        return this.idGenerator.incrementAndGet();
    }

}
