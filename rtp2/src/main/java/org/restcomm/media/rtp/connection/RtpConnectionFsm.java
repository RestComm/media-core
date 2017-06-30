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

import org.squirrelframework.foundation.fsm.StateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface RtpConnectionFsm extends StateMachine<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    void enterOpening(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);

    void exitOpening(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterParsingRemoteDescription(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitParsingRemoteDescription(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterAllocatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitAllocatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterSettingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitSesttingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterNegotiatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitNegotiatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);

    void enterGeneratingLocalDescription(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitGeneratingLocalDescription(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterOpen(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitOpen(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterCorrupted(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitCorrupted(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterUpdatingMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitUpdatingMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterUpdatingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitUpdatingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterSessionModeUpdated(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitSessionModeUpdated(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterClosing(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitClosing(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterClosingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitClosingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterClosedSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void exitClosedSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
    void enterClosed(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext);
    
}
