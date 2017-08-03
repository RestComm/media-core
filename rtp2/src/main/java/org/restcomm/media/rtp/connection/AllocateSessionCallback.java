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

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AllocateSessionCallback extends AbstractRtpConnectionActionCallback {

    public AllocateSessionCallback(RtpConnectionTransitionContext context, RtpConnectionFsm fsm) {
        super(context, fsm);
    }

    @Override
    public void onSuccess(Void result) {
        getFsm().fire(RtpConnectionEvent.ALLOCATED_SESSION, getContext());
    }

    @Override
    public void onFailure(Throwable t) {
        getContext().set(RtpConnectionTransitionParameter.ERROR, t);
        getFsm().fireImmediate(RtpConnectionEvent.FAILURE, getContext());
    }

}
