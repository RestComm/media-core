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

package org.restcomm.media.rtp.connection;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AllocateSessionCallback implements FutureCallback<Void> {

    private final RtpConnectionTransitionContext context;
    private final RtpConnectionFsm fsm;

    AllocateSessionCallback(RtpConnectionTransitionContext context, RtpConnectionFsm fsm) {
        super();
        this.context = context;
        this.fsm = fsm;
    }

    @Override
    public void onSuccess(Void result) {
        this.fsm.fire(RtpConnectionEvent.ALLOCATED_SESSION, this.context);
    }

    @Override
    public void onFailure(Throwable t) {
        this.context.set(RtpConnectionTransitionParameter.ERROR, t);
        this.fsm.fireImmediate(RtpConnectionEvent.FAILURE, this.context);
    }

}
