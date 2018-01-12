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

package org.restcomm.media.control.mgcp.connection.remote;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.control.mgcp.connection.AbstractMgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpRemoteConnection;
import org.restcomm.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.pkg.r.RtpPackage;
import org.restcomm.media.control.mgcp.pkg.r.rto.RtpTimeoutEvent;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * MGCP connection that connects one endpoint to a remote peer.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRemoteConnectionImpl extends AbstractMgcpConnection implements MgcpRemoteConnection {
    
    private static final Logger log = LogManager.getLogger(MgcpRemoteConnectionImpl.class);

    public MgcpRemoteConnectionImpl(MgcpRemoteConnectionContext context, MgcpEventProvider eventProvider, ListeningScheduledExecutorService executor) {
        super(context, eventProvider, executor);
    }

    @Override
    public boolean isLocal() {
        return false;
    }
    
    @Override
    protected MgcpRemoteConnectionContext getContext() {
        return (MgcpRemoteConnectionContext) super.getContext();
    }

    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<String> callback) {
        getContext().getRtpConnection().updateMode(mode, callback);
    }

    @Override
    public void halfOpen(LocalConnectionOptions options, FutureCallback<String> callback) {
        // TODO read lc options to know if connection is WebRTC
        getContext().getRtpConnection().halfOpen(callback);
    }

    @Override
    public void open(String sdp, FutureCallback<String> callback) {
        getContext().getRtpConnection().open(sdp, callback);
    }

    @Override
    public void negotiate(String sdp, FutureCallback<String> callback) {
        getContext().getRtpConnection().modify(sdp, callback);
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        getContext().getRtpConnection().close(callback);
    }

    @Override
    public AudioComponent getAudioComponent() {
        return getContext().getRtpConnection().getAudioComponent();
    }

    @Override
    public OOBComponent getOutOfBandComponent() {
        return getContext().getRtpConnection().getOOBComponent();
    }

    @Override
    protected boolean isEventSupported(MgcpRequestedEvent event) {
        switch (event.getPackageName()) {
            case RtpPackage.PACKAGE_NAME:
                switch (event.getEventType()) {
                    case RtpTimeoutEvent.SYMBOL:
                        return true;

                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    @Override
    protected void listen(MgcpEvent event) throws UnsupportedMgcpEventException {
        // TODO start inter-rtp timer or override existing one.
    }

    @Override
    protected Logger log() {
        return MgcpRemoteConnectionImpl.log;
    }

}
