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

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBInput;
import org.restcomm.media.component.oob.OOBOutput;
import org.restcomm.media.rtp.format.LinearFormat;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class LocalDataChannelProvider {

    private final AtomicInteger idGenerator;
    private final PriorityQueueScheduler scheduler;

    public LocalDataChannelProvider(AtomicInteger idGenerator, PriorityQueueScheduler scheduler) {
        super();
        this.idGenerator = idGenerator;
        this.scheduler = scheduler;
    }

    public LocalDataChannel provide() {
        int componentId = this.idGenerator.incrementAndGet();

        AudioComponent inbandComponent = new AudioComponent(componentId);
        AudioInput inbandInput = new AudioInput(componentId, LinearFormat.PACKET_SIZE);
        AudioOutput inbandOutput = new AudioOutput(this.scheduler, -componentId);

        OOBComponent oobComponent = new OOBComponent(componentId);
        OOBInput oobInput = new OOBInput(componentId);
        OOBOutput oobOutput = new OOBOutput(this.scheduler, -componentId);

        return new LocalDataChannel(inbandComponent, inbandInput, inbandOutput, oobComponent, oobInput, oobOutput);
    }

}
