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

import java.net.SocketAddress;

import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionImpl implements RtpSession {

    private final RtpSessionContext context;
    private final RtpSessionFsm fsm;
    
    private final RtpChannel channel;

    public RtpSessionImpl(RtpChannel channel, RtpSessionContext context) {
        this.context = context;
        this.fsm = RtpSessionFsmBuilder.INSTANCE.build(this.context);
        this.channel = channel;
    }

    @Override
    public long getSsrc() {
        return this.context.getSsrc();
    }

    @Override
    public MediaType getMediaType() {
        return this.context.getMediaType();
    }

    @Override
    public void open(SocketAddress address, FutureCallback<Void> callback) {
        RtpSessionOpenContext txContext = new RtpSessionOpenContext(this.channel, address, callback);
        this.fsm.fire(RtpSessionEvent.OPEN, txContext);
    }

    @Override
    public void negotiate(MediaDescriptionField sdp, FutureCallback<Void> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close(FutureCallback<Void> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateMode(ConnectionMode mode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void incomingRtp(RtpPacket packet) {
        // TODO Auto-generated method stub

    }

    @Override
    public void outgoingRtp(RtpPacket packet) {
        // TODO Auto-generated method stub

    }

}
