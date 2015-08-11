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
import org.mobicents.media.server.component.MediaSplitter;
import org.mobicents.media.server.component.audio.AudioForwardingSplitter;
import org.mobicents.media.server.component.audio.AudioMixingSplitter;
import org.mobicents.media.server.component.audio.MediaComponent;
import org.mobicents.media.server.component.oob.OOBSplitter;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractSplitterEndpoint extends AbstractEndpoint {

    // Media splitting
    private MediaSplitter audioSplitter;
    private OOBSplitter oobSplitter;
    private final ConcurrentMap<MediaComponent> mediaComponents;

    // IO flags
    private AtomicInteger loopbackCount = new AtomicInteger(0);
    private AtomicInteger readCount = new AtomicInteger(0);
    private AtomicInteger writeCount = new AtomicInteger(0);

    public AbstractSplitterEndpoint(String localName, RelayType relayType) {
        super(localName, relayType);
        this.mediaComponents = new ConcurrentMap<MediaComponent>();
    }

    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        // Create the connection
        AbstractConnection connection = (AbstractConnection) super.createConnection(type, isLocal);

        // Retrieve and register the mixer component of the connection
        MediaComponent mediaComponent = connection.getMediaComponent("audio");
        this.mediaComponents.put(connection.getId(), mediaComponent);

        // Add media component to the media splitter
        switch (type) {
            case RTP:
                this.audioSplitter.addOutsideComponent(mediaComponent.getInbandComponent());
                this.oobSplitter.addOutsideComponent(mediaComponent.getOOBComponent());
                break;

            case LOCAL:
                this.audioSplitter.addInsideComponent(mediaComponent.getInbandComponent());
                this.oobSplitter.addInsideComponent(mediaComponent.getOOBComponent());
                break;
        }
        return connection;
    }

    @Override
    public void deleteConnection(Connection connection) {
        // Release the connection
        super.deleteConnection(connection);

        // Unregister the media component of the connection
        MediaComponent mediaComponent = this.mediaComponents.remove(connection.getId());

        // Release the media component from the media splitter
        switch (connection.getConnectionType()) {
            case RTP:
                this.audioSplitter.removeOutsideComponent(mediaComponent.getInbandComponent());
                this.oobSplitter.releaseOutsideComponent(mediaComponent.getOOBComponent());
                break;

            case LOCAL:
                this.audioSplitter.removeInsideComponent(mediaComponent.getInbandComponent());
                this.oobSplitter.releaseInsideComponent(mediaComponent.getOOBComponent());
                break;
        }
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
                this.audioSplitter.stop();
                this.oobSplitter.stop();
            } else {
                this.audioSplitter.start();
                this.oobSplitter.start();
            }
        }
    }

    @Override
    public void start() throws ResourceUnavailableException {
        switch (getRelayType()) {
            case MIXER:
                this.audioSplitter = new AudioMixingSplitter(getScheduler());
                break;
            case TRANSLATOR:
                this.audioSplitter = new AudioForwardingSplitter(getScheduler());
                break;
            default:
                throw new ResourceUnavailableException("The media splitter is not available for the given relay type: "
                        + getRelayType());
        }
        this.oobSplitter = new OOBSplitter(getScheduler());
        super.start();
    }

    @Override
    public void stop() {
        // stop the endpoint
        super.stop();

        // stop the media splitter
        this.audioSplitter.stop();
        this.oobSplitter.stop();
    }

}
