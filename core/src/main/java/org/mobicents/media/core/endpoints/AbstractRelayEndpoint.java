/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.core.endpoints;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.core.connections.AbstractConnection;
import org.mobicents.media.server.component.MediaRelay;
import org.mobicents.media.server.component.OobRelay;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.audio.AudioTranslator;
import org.mobicents.media.server.component.audio.MediaComponent;
import org.mobicents.media.server.component.oob.OOBMixer;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Generic endpoint that allows the user to select the relay type of the media streams: mixing or forwarding.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractRelayEndpoint extends AbstractEndpoint {

    // Media relay
    protected MediaRelay audioRelay;
    protected OobRelay oobRelay;
    private final ConcurrentMap<MediaComponent> audioComponents;

    // IO flags
    private AtomicInteger loopbackCount = new AtomicInteger(0);
    private AtomicInteger readCount = new AtomicInteger(0);
    private AtomicInteger writeCount = new AtomicInteger(0);

    public AbstractRelayEndpoint(String localName, RelayType relayType) {
        super(localName, relayType);
        this.audioComponents = new ConcurrentMap<MediaComponent>();
    }

    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        // Create the connection
        AbstractConnection connection = (AbstractConnection) super.createConnection(type, isLocal);

        // Retrieve and register the mixer component of the connection
        MediaComponent mediaComponent = connection.getMediaComponent("audio");
        this.audioComponents.put(connection.getId(), mediaComponent);

        // Add mixing component to the media mixer
        this.audioRelay.addComponent(mediaComponent.getAudioComponent());
        this.oobRelay.addComponent(mediaComponent.getOOBComponent());
        return connection;
    }
    
    @Override
    public void deleteConnection(Connection connection) {
        // Release the connection
        super.deleteConnection(connection);

        // Unregister the mixer component of the connection
        MediaComponent mixerComponent = this.audioComponents.remove(connection.getId());

        // Release the mixing component from the media mixer
        this.audioRelay.removeComponent(mixerComponent.getAudioComponent());
        this.oobRelay.removeComponent(mixerComponent.getOOBComponent());
    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        int readCount = 0, loopbackCount = 0, writeCount = 0;
        switch (oldMode) {
            case RECV_ONLY:
                readCount -= 1;
                break;
            case SEND_ONLY:
                writeCount -= 1;
                break;
            case SEND_RECV:
            case CONFERENCE:
                readCount -= 1;
                writeCount -= 1;
                break;
            case NETWORK_LOOPBACK:
                loopbackCount -= 1;
                break;
            default:
                // XXX handle default case
                break;
        }

        switch (newMode) {
            case RECV_ONLY:
                readCount += 1;
                break;
            case SEND_ONLY:
                writeCount += 1;
                break;
            case SEND_RECV:
            case CONFERENCE:
                readCount += 1;
                writeCount += 1;
                break;
            case NETWORK_LOOPBACK:
                loopbackCount += 1;
                break;
            default:
                // XXX handle default case
                break;
        }

        if (readCount != 0 || writeCount != 0 || loopbackCount != 0) {
            // something changed
            loopbackCount = this.loopbackCount.addAndGet(loopbackCount);
            readCount = this.readCount.addAndGet(readCount);
            writeCount = this.writeCount.addAndGet(writeCount);

            if (loopbackCount > 0 || readCount == 0 || writeCount == 0) {
                this.audioRelay.stop();
                this.oobRelay.stop();
            } else {
                this.audioRelay.start();
                this.oobRelay.start();
            }
        }
    }

    @Override
    public void start() throws ResourceUnavailableException {
        // Initialize media relay according to relay type
        switch (getRelayType()) {
            case MIXER:
                this.audioRelay = new AudioMixer(getScheduler());
                break;
            case TRANSLATOR:
                this.audioRelay = new AudioTranslator(getScheduler());
                break;
            default:
                throw new ResourceUnavailableException("The media relay is not available for the given relay type: "
                        + getRelayType());
        }
        this.oobRelay = new OOBMixer(getScheduler());

        // Start the endpoint
        super.start();
    }

    @Override
    public void stop() {
        // Stop the endpoint
        super.stop();

        // Stop the media relay
        this.audioRelay.stop();
        this.oobRelay.stop();
    }

}
