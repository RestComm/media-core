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

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RtpSession;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CloseSessionCallback extends AbstractRtpConnectionActionCallback {

    private final Logger log = Logger.getLogger(CloseSessionCallback.class);

    public CloseSessionCallback(RtpConnectionTransitionContext context, RtpConnectionFsm fsm) {
        super(context, fsm);
    }

    @Override
    public void onSuccess(Void result) {
        getFsm().fire(RtpConnectionEvent.SESSION_CLOSED, getContext());

    }

    @Override
    public void onFailure(Throwable t) {
        final RtpConnectionContext globalContext = getFsm().getContext();
        final String cname = globalContext.getCname();
        final RtpSession session = getContext().get(RtpConnectionTransitionParameter.RTP_SESSION, RtpSession.class);

        log.error("RTP Connection " + cname + " could not close session " + session.getSsrc(), t);

        getFsm().fire(RtpConnectionEvent.SESSION_CLOSED, getContext());
    }

}
