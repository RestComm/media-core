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

package org.mobicents.media.server.component.audio;

import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.MediaInput;
import org.mobicents.media.server.component.MediaOutput;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * Contains the media and out-of-band components of a connection.
 * 
 * @author Henrique Rosa (henrique.rosa@gmail.com)
 *
 */
public class MediaComponent {

    private final int channelId;
    private final InbandComponent inbandComponent;
    private final OOBComponent oobComponent;

    public MediaComponent(int channelId) {
        this.channelId = channelId;
        this.inbandComponent = new InbandComponent(channelId);
        this.oobComponent = new OOBComponent(channelId);
    }

    public int getChannelId() {
        return channelId;
    }

    public InbandComponent getInbandComponent() {
        return inbandComponent;
    }

    public void addInput(MediaInput input) {
        this.inbandComponent.addInput(input);
    }

    public void addOutput(MediaOutput output) {
        this.inbandComponent.addOutput(output);
    }

    public OOBComponent getOOBComponent() {
        return oobComponent;
    }

    public void addOOBInput(OOBInput input) {
        this.oobComponent.addInput(input);
    }

    public void addOOBOutput(OOBOutput output) {
        this.oobComponent.addOutput(output);
    }

    public void updateMode(ConnectionMode connectionMode) {
        boolean readable;
        boolean writable;

        switch (connectionMode) {
            case SEND_ONLY:
                readable = false;
                writable = true;
                break;
            case RECV_ONLY:
                readable = true;
                writable = false;
                break;
            case SEND_RECV:
            case CONFERENCE:
                readable = true;
                writable = true;
                break;
            case INACTIVE:
            case NETWORK_LOOPBACK:
            default:
                readable = false;
                writable = false;
                break;
        }

        this.inbandComponent.setReadable(readable);
        this.inbandComponent.setWritable(writable);
        this.oobComponent.setReadable(readable);
        this.oobComponent.setWritable(writable);
    }

}
