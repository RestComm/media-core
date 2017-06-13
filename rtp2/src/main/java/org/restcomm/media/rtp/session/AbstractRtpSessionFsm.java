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

package org.restcomm.media.rtp.session;

import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AbstractRtpSessionFsm
        extends AbstractStateMachine<RtpSessionFsm, RtpSessionState, RtpSessionEvent, RtpSessionTransactionContext>
        implements RtpSessionFsm {

    @Override
    public void enterOpening(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitOpening(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }
    
    @Override
    public void enterAllocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }
    
    @Override
    public void exitAllocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterBinding(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitBinding(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterOpened(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }
    
    @Override
    public void exitOpened(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterOpen(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitOpen(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterNegotiating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitNegotiating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterNegotiatingFormats(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitNegotiatingFormats(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterNegotiated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitNegotiated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }
    
    @Override
    public void enterNegotiationFailed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }
    
    @Override
    public void exitNegotiationFailed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterEstablished(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitEstablished(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void onUpdateMode(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void onIncomingRtp(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void onOutgoingRtp(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void enterClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

    @Override
    public void exitClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
    }

}
