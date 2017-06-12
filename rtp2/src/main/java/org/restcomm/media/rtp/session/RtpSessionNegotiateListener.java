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

import org.restcomm.media.rtp.session.exception.RtpSessionException;
import org.squirrelframework.foundation.exception.TransitionException;
import org.squirrelframework.foundation.fsm.Action;
import org.squirrelframework.foundation.fsm.annotation.OnActionExecException;
import org.squirrelframework.foundation.fsm.annotation.OnTransitionBegin;
import org.squirrelframework.foundation.fsm.annotation.OnTransitionComplete;
import org.squirrelframework.foundation.fsm.annotation.OnTransitionDecline;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionNegotiateListener extends AbstractRtpSessionFsmListener {

    private final FutureCallback<Void> callback;

    public RtpSessionNegotiateListener(RtpSessionFsm fsm, FutureCallback<Void> callback) {
        super(fsm);
        this.callback = callback;
    }

    @Override
    protected void succeeded(Void result) {
        this.callback.onSuccess(null);
    }

    @Override
    protected void failed(Throwable t) {
        this.callback.onFailure(t);
    }

    @OnTransitionBegin
    public void transitionBegin(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        if (context != null) {
            FutureCallback<Void> originator = context.getCallback();
            if (originator == this.callback) {
                if (RtpSessionState.CLOSED.equals(to)) {
                    RtpSessionException exception = new RtpSessionException("Error caused session to close prematurely.");
                    onFailure(exception);
                }
            }
        }
    }

    @OnTransitionComplete
    public void transitionComplete(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        if (context != null) {
            FutureCallback<Void> originator = context.getCallback();
            if (this.callback == originator) {
                if (RtpSessionState.NEGOTIATED.equals(to)) {
                    onSuccess(null);
                }
            }
        }
    }

    @OnTransitionDecline
    public void transitionDeclined(RtpSessionState from, RtpSessionEvent event, RtpSessionTransactionContext context) {
        if (context != null) {
            FutureCallback<Void> originator = context.getCallback();
            if (this.callback == originator) {
                RtpSessionException exception = new RtpSessionException("Action declined " + event);
                onFailure(exception);
            }
        }
    }

    @OnActionExecException
    public void onActionExecException(RtpSessionState from, RtpSessionEvent event, RtpSessionTransactionContext context, Action<?, ?, ?, ?> action, TransitionException e) {
        if (context != null) {
            FutureCallback<Void> originator = context.getCallback();
            if (this.callback == originator) {
                RtpSessionException exception = new RtpSessionException("Error executing action " + event, e);
                onFailure(exception);
            }
        }
    }

}
