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

package org.restcomm.media.control.mgcp.command.rqnt;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 24/11/2017
 */
public class ExecuteActionCallback implements FutureCallback<Void> {

    private static final Logger log = Logger.getLogger(ExecuteActionCallback.class);

    private final RequestNotificationFsm fsm;
    private final RequestNotificationContext context;

    ExecuteActionCallback(RequestNotificationFsm fsm, RequestNotificationContext context) {
        this.fsm = fsm;
        this.context = context;
    }

    @Override
    public void onSuccess(Void result) {
        this.fsm.fireImmediate(RequestNotificationEvent.EXECUTED, context);
        if (log.isDebugEnabled()) {
            log.debug("MGCP RQNT tx=" + context.getTransactionId() + "successfully requested notification from endpoint " + context.getEndpointId());
        }
    }

    @Override
    public void onFailure(Throwable t) {
        this.context.setError(t);
        this.fsm.fireImmediate(RequestNotificationEvent.FAILURE, context);
        log.warn("MGCP RQNT tx=" + context.getTransactionId() + "failed to request notification from endpoint " + context.getEndpointId());
    }

}
