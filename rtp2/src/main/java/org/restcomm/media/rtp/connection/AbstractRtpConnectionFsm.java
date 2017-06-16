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

import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractRtpConnectionFsm extends AbstractStateMachine<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> implements RtpConnectionFsm {

    @Override
    public void enterOpening(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitOpening(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterAllocatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitAllocatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterSettingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitSesttingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterNegotiatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitNegotiatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterSessionEstablished(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitSessionEstablished(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterOpen(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitOpen(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterCorrupted(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitCorrupted(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterUpdatingMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitUpdatingMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterUpdatingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitUpdatingSessionMode(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterSessionModeUpdated(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitSessionModeUpdated(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterClosing(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitClosing(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterClosingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitClosingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterClosedSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void exitClosedSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

    @Override
    public void enterClosed(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event,
            RtpConnectionTransitionContext txContext) {
    }

}
