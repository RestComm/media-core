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

package org.restcomm.media.control.mgcp.endpoint.notification;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SignalExecutionCallback<T> implements FutureCallback<T> {
    
    private static final Logger log = Logger.getLogger(SignalExecutionCallback.class);

    private final MgcpSignal<T> signal;
    private final NotificationCenterFsm fsm;

    public SignalExecutionCallback(MgcpSignal<T> signal, NotificationCenterFsm fsm) {
        this.signal = signal;
        this.fsm = fsm;
    }

    @Override
    public void onSuccess(T result) {
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, result);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, this.signal);
        this.fsm.fire(NotificationCenterEvent.SIGNAL_EXECUTED, txContext);
        
        if(log.isDebugEnabled()) {
            final NotificationCenterContext context = this.fsm.getContext();
            final String endpointId = context.getEndpoint().getEndpointId().toString();
            log.debug("Signal " + this.signal + " finished executing on endpoint " + endpointId);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.FAILED_SIGNAL, this.signal);
        txContext.set(NotificationCenterTransitionParameter.ERROR, this.signal);
        this.fsm.fire(NotificationCenterEvent.SIGNAL_FAILED, txContext);
        
        if(log.isDebugEnabled()) {
            final NotificationCenterContext context = this.fsm.getContext();
            final String endpointId = context.getEndpoint().getEndpointId().toString();
            log.debug("Signal " + this.signal + " failed on endpoint " + endpointId, t);
        }
    }

}
