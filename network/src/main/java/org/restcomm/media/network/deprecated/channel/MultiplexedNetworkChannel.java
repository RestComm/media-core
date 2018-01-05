/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.network.deprecated.channel;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MultiplexedNetworkChannel extends AbstractNetworkChannel {
    
    private static final Logger log = LogManager.getLogger(MultiplexedNetworkChannel.class);

    protected final PacketHandlerPipeline handlers;

    public MultiplexedNetworkChannel(NetworkGuard guard, PacketHandler... handlers) {
        super(guard);
        this.handlers = new PacketHandlerPipeline();
        for (PacketHandler handler : handlers) {
            this.handlers.addHandler(handler);
        }
    }

    @Override
    protected void onIncomingPacket(byte[] data, InetSocketAddress remotePeer) {
        // Get appropriate handler to process incoming packet
        final PacketHandler handler = this.handlers.getHandler(data);

        if (handler == null) {
            if (log().isDebugEnabled()) {
                log().debug("No protocol handler was found to process an incoming packet. Packet will be dropped.");
            }
        } else {
            try {
                // Let the handler process the incoming packet. A response MAY be provided as result.
                final byte[] response = handler.handle(data, this.getLocalAddress(), remotePeer);
                if (response != null && response.length > 0) {
                    send(response, remotePeer);
                }
            } catch (PacketHandlerException e) {
                log().warn(handler.getClass().getSimpleName() + " could not handle incoming packet: " + e.getMessage());
            } catch (IOException e) {
                log().warn(handler.getClass().getSimpleName() + " could not send response to remote peer " + remotePeer.getAddress().toString());
            }
        }
    }
    
    @Override
    protected Logger log() {
        return log;
    }

}
