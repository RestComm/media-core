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
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.oob.OOBMixer;
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
public class BaseMixerEndpointImpl extends BaseEndpointImpl {

    protected AudioMixer audioMixer;
    protected OOBMixer oobMixer;

    private ConnectionMode mode;

    public BaseMixerEndpointImpl(String localName) {
        super(localName);
        this.mode = ConnectionMode.INACTIVE;
    }

    @Override
    public void start() throws ResourceUnavailableException {
        super.start();
        audioMixer = new AudioMixer(getScheduler());
        oobMixer = new OOBMixer(getScheduler());
    }

    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        Connection connection = super.createConnection(type, isLocal);
        audioMixer.addComponent(((BaseConnection) connection).getAudioComponent());
        oobMixer.addComponent(((BaseConnection) connection).getOOBComponent());
        return connection;
    }

    @Override
    public void deleteConnection(Connection connection, ConnectionType connectionType) {
        super.deleteConnection(connection, connectionType);
        audioMixer.release(((BaseConnection) connection).getAudioComponent());
        oobMixer.release(((BaseConnection) connection).getOOBComponent());
    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        if (!this.mode.equals(newMode)) {
            switch (newMode) {
                case RECV_ONLY:
                case SEND_ONLY:
                case SEND_RECV:
                case CONFERENCE:
                    if (!this.audioMixer.isStarted()) {
                        this.audioMixer.start();
                    }

                    if (!this.oobMixer.isStarted()) {
                        this.oobMixer.start();
                    }
                    break;

                default:
                    if (this.audioMixer.isStarted()) {
                        this.audioMixer.stop();
                    }

                    if (this.oobMixer.isStarted()) {
                        this.oobMixer.stop();
                    }
                    break;
            }
            this.mode = newMode;
        }
    }
}
