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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;

import org.mobicents.media.server.component.MediaInput;
import org.mobicents.media.server.component.MediaOutput;
import org.mobicents.media.server.component.audio.MediaComponent;
import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 * Local Channel implementation. Bridge between 2 endpoints.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class LocalChannel {

    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    private final MediaInput inbandInput;
    private final MediaOutput inbandOutput;

    private final OOBInput oobInput;
    private final OOBOutput oobOutput;

    // Media relay
    private final MediaComponent mediaComponent;

    private LocalChannel otherChannel = null;

    protected LocalChannel(ChannelsManager channelsManager, int channelId) {
        this.inbandInput = new MediaInput(1, PACKET_SIZE);
        this.inbandOutput = new MediaOutput(2, channelsManager.getScheduler());
        this.oobInput = new OOBInput(1);
        this.oobOutput = new OOBOutput(channelsManager.getScheduler(), 2);

        // Media relay
        this.mediaComponent = new MediaComponent(channelId);
        this.mediaComponent.addInput(inbandInput);
        this.mediaComponent.addOutput(inbandOutput);
        this.mediaComponent.addOOBInput(oobInput);
        this.mediaComponent.addOOBOutput(oobOutput);
    }

    public MediaComponent getMediaComponent() {
        return mediaComponent;
    }

    public void join(LocalChannel otherChannel) throws IOException {
        if (this.otherChannel != null) {
            throw new IOException("Channel already joined");
        }

        this.otherChannel = otherChannel;
        otherChannel.otherChannel = this;

        this.otherChannel.inbandOutput.join(inbandInput);
        this.otherChannel.oobOutput.join(oobInput);
        this.inbandOutput.join(this.otherChannel.inbandInput);
        this.oobOutput.join(this.otherChannel.oobInput);
    }

    public void unjoin() {
        if (this.otherChannel == null) {
            return;
        }

        this.inbandOutput.deactivate();
        this.oobOutput.deactivate();
        this.inbandOutput.unjoin();
        oobOutput.unjoin();

        this.otherChannel = null;
    }

    public void updateMode(ConnectionMode connectionMode) throws ModeNotSupportedException {
        if (this.otherChannel == null) {
            throw new ModeNotSupportedException("You should join channel first");
        }

        // Update audio and OOB components
        mediaComponent.updateMode(connectionMode);

        switch (connectionMode) {
            case SEND_ONLY:
                inbandOutput.activate();
                oobOutput.activate();
                break;
            case RECV_ONLY:
                inbandOutput.deactivate();
                oobOutput.deactivate();
                break;
            case INACTIVE:
                inbandOutput.deactivate();
                oobOutput.deactivate();
                break;
            case SEND_RECV:
            case CONFERENCE:
                inbandOutput.activate();
                oobOutput.activate();
                break;
            case NETWORK_LOOPBACK:
                throw new ModeNotSupportedException("Loopback not supported on local channel");
            default:
                // XXX handle default case
                break;
        }
    }

}