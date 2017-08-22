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

package org.restcomm.media.control.mgcp.endpoint;

import java.util.Set;

import org.apache.log4j.Logger;
import org.squirrelframework.foundation.fsm.annotation.OnTransitionComplete;
import org.squirrelframework.foundation.fsm.annotation.OnTransitionDecline;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointFsmListener {
    
    private static final Logger log = Logger.getLogger(MgcpEndpointFsmListener.class);

    private final MgcpEndpoint endpoint;
    private final MgcpEndpointContext context;

    public MgcpEndpointFsmListener(MgcpEndpoint endpoint, MgcpEndpointContext context) {
        super();
        this.endpoint = endpoint;
        this.context = context;
    }

    @OnTransitionComplete
    public void transitionComplete(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext txContext) {
        if (from != null) {
            Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
            
            if (log.isTraceEnabled()) {
                EndpointIdentifier endpointId = context.getEndpointId();
                log.trace("Endpoint " + endpointId + "is notifying observers that state changed to " + to.name());
            }
            
            for (MgcpEndpointObserver observer : observers) {
                observer.onEndpointStateChanged(this.endpoint, to);
            }
        }
    }
    
    @OnTransitionDecline
    public void transitionDeclined(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext txContext) {
        FutureCallback<?> callback = txContext.get(MgcpEndpointParameter.CALLBACK, FutureCallback.class);
        
        if(callback != null) {
            Throwable t = new IllegalStateException("Endpoint " + this.context.getEndpointId() + " denied operation " + event.name());
            callback.onFailure(t);
        }
    }

}
