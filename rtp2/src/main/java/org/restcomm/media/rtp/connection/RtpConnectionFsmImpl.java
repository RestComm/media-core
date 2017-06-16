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

import java.net.SocketAddress;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.rtp.connection.exception.SessionAllocationException;
import org.restcomm.media.rtp.connection.exception.SessionNegotiationException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionFsmImpl extends AbstractRtpConnectionFsm {

    private static final Logger log = Logger.getLogger(RtpConnectionFsmImpl.class);

    private final RtpConnectionContext context;

    public RtpConnectionFsmImpl(RtpConnectionContext context) {
        super();
        this.context = context;
    }

    @Override
    public void enterAllocatingSession(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext) {
        // Get relevant data from context
        final RtpConnectionOpenContext openContext = (RtpConnectionOpenContext) txContext;
        final RtpSession session = openContext.getSession();
        final SocketAddress address = openContext.getAddress();

        // Open session. The callback will fire proper event to move to next state.
        AllocateSessionCallback callback = new AllocateSessionCallback(this, openContext);
        session.open(address, callback);
    }

    @Override
    public void enterOpen(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext) {
        final RtpConnectionOpenContext openContext = (RtpConnectionOpenContext) txContext;

        if (log.isDebugEnabled()) {
            final RtpSession session = openContext.getSession();
            final String cname = this.context.getCname();
            log.debug("RTP Connection " + cname + " opened " + session.getMediaType().name() + "session " + session.getSsrc());
        }

        // Let originator of the OPEN request know the operation completed successfully
        openContext.getOriginator().onSuccess(null);
    }

    @Override
    public void enterCorrupted(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext txContext) {
        final RtpConnectionOpenContext openContext = (RtpConnectionOpenContext) txContext;
        final RtpConnectionException exception;
        
        switch (event) {
            case SESSION_ALLOCATION_FAILURE: {
                final RtpSession session = openContext.getSession();
                final String cname = this.context.getCname();
                final Throwable t = openContext.getThrowable();
                exception = new SessionAllocationException("RTP Connection " + cname + " could not allocate session " + session.getSsrc(), t);
                break;
            }

            case SESSION_NEGOTIATION_FAILURE: {
                final RtpSession session = openContext.getSession();
                final String cname = this.context.getCname();
                final Throwable t = openContext.getThrowable();
                exception = new SessionNegotiationException("RTP Connection " + cname + " could not negotiate session " + session.getSsrc(), t);
                break;
            }

            default: {
                final String cname = this.context.getCname();
                final Throwable t = openContext.getThrowable();
                exception = new RtpConnectionException("RTP Connection " + cname + " became corrupted. Reason: " + event.name(), t);
                break;
            }
        }

        // Let originator of the OPEN request know the operation failed
        openContext.getOriginator().onFailure(exception);
    }

}
