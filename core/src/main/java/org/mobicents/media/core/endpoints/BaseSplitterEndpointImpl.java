/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.core.endpoints;

import org.mobicents.media.core.connections.BaseConnection;
import org.mobicents.media.server.component.audio.AudioSplitter;
import org.mobicents.media.server.component.oob.OOBSplitter;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public class BaseSplitterEndpointImpl extends BaseEndpointImpl {

    protected AudioSplitter audioSplitter;
    protected OOBSplitter oobSplitter;

    private ConnectionMode mode;

    public BaseSplitterEndpointImpl(String localName) {
        super(localName);
        this.mode = ConnectionMode.INACTIVE;
    }

    @Override
    public void start() throws ResourceUnavailableException {
        super.start();
        audioSplitter = new AudioSplitter(getScheduler());
        oobSplitter = new OOBSplitter(getScheduler());
    }

    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        Connection connection = super.createConnection(type, isLocal);

        switch (type) {
            case RTP:
                audioSplitter.addOutsideComponent(((BaseConnection) connection).getAudioComponent());
                oobSplitter.addOutsideComponent(((BaseConnection) connection).getOOBComponent());
                break;
            case LOCAL:
                audioSplitter.addInsideComponent(((BaseConnection) connection).getAudioComponent());
                oobSplitter.addInsideComponent(((BaseConnection) connection).getOOBComponent());
                break;
        }
        return connection;
    }

    @Override
    public void deleteConnection(Connection connection, ConnectionType connectionType) {
        super.deleteConnection(connection, connectionType);

        switch (connectionType) {
            case RTP:
                audioSplitter.releaseOutsideComponent(((BaseConnection) connection).getAudioComponent());
                oobSplitter.releaseOutsideComponent(((BaseConnection) connection).getOOBComponent());
                break;
            case LOCAL:
                audioSplitter.releaseInsideComponent(((BaseConnection) connection).getAudioComponent());
                oobSplitter.releaseInsideComponent(((BaseConnection) connection).getOOBComponent());
                break;
        }
    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        if (!this.mode.equals(newMode)) {
            switch (newMode) {
                case RECV_ONLY:
                case SEND_ONLY:
                case SEND_RECV:
                case CONFERENCE:
                    if (!this.audioSplitter.isStarted()) {
                        this.audioSplitter.start();
                    }

                    if (!this.oobSplitter.isStarted()) {
                        this.oobSplitter.start();
                    }
                    break;

                default:
                    if (this.audioSplitter.isStarted()) {
                        this.audioSplitter.stop();
                    }

                    if (this.oobSplitter.isStarted()) {
                        this.oobSplitter.stop();
                    }
                    break;
            }
            this.mode = newMode;
        }
    }
}
