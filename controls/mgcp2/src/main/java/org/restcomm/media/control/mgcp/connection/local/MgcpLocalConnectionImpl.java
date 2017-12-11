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
import org.restcomm.media.control.mgcp.connection.MgcpLocalConnection;
import org.restcomm.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnectionImpl extends AbstractMgcpConnection implements MgcpLocalConnection {

    private static final Logger log = Logger.getLogger(MgcpLocalConnectionImpl.class);

    private final MgcpLocalConnectionFsm fsm;

    public MgcpLocalConnectionImpl(MgcpLocalConnectionContext context, MgcpEventProvider eventProvider, ListeningScheduledExecutorService executor, MgcpLocalConnectionFsmBuilder fsmBuilder) {
        super(context, eventProvider, executor);
        this.fsm = fsmBuilder.build(context);
        this.fsm.start();
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
    public void halfOpen(LocalConnectionOptions options, FutureCallback<String> callback) {
        MgcpLocalConnectionEvent event = MgcpLocalConnectionEvent.HALF_OPEN;
        if (this.fsm.canAccept(event)) {
            // Build transition context
            MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
            txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);
            txContext.set(MgcpLocalConnectionParameter.TIMEOUT, getContext().getHalfOpenTimeout());
            txContext.set(MgcpLocalConnectionParameter.SCHEDULER, getExecutor());
            
            // Fire event
            this.fsm.fire(event, txContext);
        } else {
            denyOperation(event, callback);
        }
    }

    @Override
    public void open(String sdp, FutureCallback<String> callback) {
        MgcpLocalConnectionEvent event = MgcpLocalConnectionEvent.OPEN;
        if (this.fsm.canAccept(event)) {
            // Build transition context
            MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
            txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);
            txContext.set(MgcpLocalConnectionParameter.TIMEOUT, getContext().getTimeout());
            txContext.set(MgcpLocalConnectionParameter.SCHEDULER, getExecutor());
            
            // Fire event
            this.fsm.fire(event, txContext);
        } else {
            denyOperation(event, callback);
        }
    }

    @Override
    public void negotiate(String sdp, FutureCallback<String> callback) {
        denyOperation(MgcpLocalConnectionEvent.RENEGOTIATE, callback);
    }
    
    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<Void> callback) {
        MgcpLocalConnectionEvent event = MgcpLocalConnectionEvent.UPDATE_MODE;
        if (this.fsm.canAccept(event)) {
            // Build transition context
            MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
            txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);
            txContext.set(MgcpLocalConnectionParameter.MODE, mode);

            // Fire event
            this.fsm.fire(event, txContext);
        } else {
            denyOperation(event, callback);
        }
    }

    @Override
    public void join(MgcpLocalConnection connection, FutureCallback<Void> callback) {
        MgcpLocalConnectionEvent event = MgcpLocalConnectionEvent.JOIN;
        if (this.fsm.canAccept(event)) {
            // Build transition context
            MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
            txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);
            txContext.set(MgcpLocalConnectionParameter.JOINEE, connection);
            
            // Fire event
            this.fsm.fire(event, txContext);
        } else {
            denyOperation(event, callback);
        }
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        MgcpLocalConnectionEvent event = MgcpLocalConnectionEvent.CLOSE;
        if (this.fsm.canAccept(event)) {
            // Build transition context
            MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
            txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);

            // Fire event
            this.fsm.fire(event, txContext);
        } else {
            denyOperation(event, callback);
        }
    }

    private void denyOperation(MgcpLocalConnectionEvent event, FutureCallback<?> callback) {
        Throwable t = new IllegalArgumentException("MGCP Connection " + getContext().getHexIdentifier() + " denied operation " + event.name());
        callback.onFailure(t);
    }

    @Override
    public AudioComponent getAudioComponent() {
        final LocalDataChannel audioChannel = getContext().getAudioChannel();
        return (audioChannel == null) ? null : audioChannel.getInbandComponent();
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
        return MgcpLocalConnectionImpl.log;
    }

}
