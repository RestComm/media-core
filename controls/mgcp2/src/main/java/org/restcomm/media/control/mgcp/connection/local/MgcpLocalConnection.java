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

package org.restcomm.media.control.mgcp.connection.local;

import org.apache.log4j.Logger;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.control.mgcp.connection.AbstractMgcpConnection;
import org.restcomm.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.rtp.LocalDataChannel;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnection extends AbstractMgcpConnection {

    private static final Logger log = Logger.getLogger(MgcpLocalConnection.class);

    public MgcpLocalConnection(MgcpLocalConnectionContext context, MgcpEventProvider eventProvider,
            ListeningScheduledExecutorService executor, ConnectionMode mode, LocalDataChannel audioChannel) {
        super(context, eventProvider, executor);
    }

    @Override
    protected MgcpLocalConnectionContext getContext() {
        return (MgcpLocalConnectionContext) super.getContext();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<Void> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void halfOpen(LocalConnectionOptions options, FutureCallback<String> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void open(String sdp, FutureCallback<String> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void renegotiate(String sdp, FutureCallback<String> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close(FutureCallback<Void> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public AudioComponent getAudioComponent() {
        final LocalDataChannel audioChannel = getContext().getAudioChannel();
        return (audioChannel == null) ? null : audioChannel.getAudioComponent();
    }

    @Override
    public OOBComponent getOutOfBandComponent() {
        final LocalDataChannel audioChannel = getContext().getAudioChannel();
        return (audioChannel == null) ? null : audioChannel.getOOBComponent();
    }

    @Override
    protected boolean isEventSupported(MgcpRequestedEvent event) {
        return false;
    }

    @Override
    protected void listen(MgcpEvent event) throws UnsupportedMgcpEventException {
        throw new UnsupportedMgcpEventException("Local connection " + this.getHexIdentifier() + " does not support event " + event.toString());
    }

    @Override
    protected Logger log() {
        return MgcpLocalConnection.log;
    }

}
