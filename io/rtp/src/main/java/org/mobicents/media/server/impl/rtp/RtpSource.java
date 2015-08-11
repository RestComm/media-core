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

package org.mobicents.media.server.impl.rtp;

import org.mobicents.media.server.component.MediaInput;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Implementation of a media source.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Oifa Yulian
 *
 */
public class RtpSource extends AbstractSource implements BufferListener {

    private static final long serialVersionUID = -434214678421348922L;

    // Media mixing components
    private final JitterBuffer jitterBuffer;
    private final MediaInput mediaInput;

    public RtpSource(Scheduler scheduler, JitterBuffer jitterBuffer) {
        super("input", scheduler, Scheduler.INPUT_QUEUE);

        // Media mixing components
        this.jitterBuffer = jitterBuffer;
        this.jitterBuffer.setListener(this);
        // this.audioInput = new MediaInput(1, PACKET_SIZE);
        this.mediaInput = new MediaInput(1);
        connect(mediaInput);
    }

    public MediaInput getMediaInput() {
        return mediaInput;
    }

    @Override
    public Frame evolve(long timestamp) {
        return this.jitterBuffer.read(timestamp);
    }

    @Override
    public void onFill() {
        this.wakeup();
    }

}
