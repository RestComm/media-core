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

import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * Finite State Machine that manages an RTP Session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface RtpSessionFsm extends StateMachine<RtpSessionFsm, RtpSessionState, RtpSessionEvent, RtpSessionTransactionContext> {

    void enterOpening(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitOpening(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterAllocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitAllocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterBinding(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void exitBinding(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterOpened(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitOpened(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterOpen(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitOpen(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterNegotiating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitNegotiating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterNegotiatingFormats(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitNegotiatingFormats(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void exitConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void enterNegotiated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitNegotiated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void enterEstablished(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);
    
    void exitEstablished(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void onUpdateMode(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void onIncomingRtp(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void onOutgoingRtp(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void enterClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

    void exitClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context);

}
