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

package org.restcomm.media.control.mgcp.endpoint.factory;

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.control.mgcp.endpoint.naming.EndpointNameGenerator;
import org.restcomm.media.spi.Endpoint;

/**
 * Produces MGCP endpoints
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractEndpointFactory<T extends Endpoint> implements EndpointFactory<T> {
    
    protected final EndpointNameGenerator nameParser;
    protected final AtomicInteger idGenerator;
    protected final String endpointPattern;
    protected final int initialSize;
    
    public AbstractEndpointFactory(String endpointPattern, int initialSize) {
        this.idGenerator = new AtomicInteger(1);
        this.endpointPattern = endpointPattern;
        this.initialSize = initialSize;
        this.nameParser = new EndpointNameGenerator();
        this.nameParser.setPattern(endpointPattern);
    }
    
    public AbstractEndpointFactory(String endpointPattern) {
        this(endpointPattern, 0);
    }
    
    public String getEndpointPattern() {
        return endpointPattern;
    }
    
    protected String generateName() {
        return this.endpointPattern + this.idGenerator.getAndIncrement();
    }

}
