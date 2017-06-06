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
        
package org.restcomm.media.rtp.handler;

import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AbstractRtpInboundHandlerFsm extends AbstractStateMachine<RtpInboundHandlerFsm, RtpInboundHandlerState, RtpInboundHandlerEvent, RtpInboundHandlerTransactionContext> implements RtpInboundHandlerFsm {

    @Override
    public void enterActivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
    }

    @Override
    public void exitActivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
    }

    @Override
    public void enterDeactivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
    }

    @Override
    public void exitDeactivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
    }

    @Override
    public void onPacketReceived(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
    }

}
