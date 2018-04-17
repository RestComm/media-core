/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
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

package org.restcomm.media.core.resource.dtmf;

import org.restcomm.media.core.component.audio.AudioOutput;
import org.restcomm.media.core.component.oob.OOBOutput;
import org.restcomm.media.core.resource.dtmf.DtmfDetectorProvider;
import org.restcomm.media.core.resource.dtmf.DtmfSinkFacade;
import org.restcomm.media.core.resource.dtmf.DtmfSinkFacadeProvider;
import org.restcomm.media.core.resource.dtmf.InbandDtmfSink;
import org.restcomm.media.core.resource.dtmf.Rfc2833DtmfSink;
import org.restcomm.media.core.resource.dtmf.Rfc2833DtmfDetector;
import org.restcomm.media.core.scheduler.PriorityQueueScheduler;
import org.restcomm.media.core.spi.ComponentType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides instances of {@link DtmfSinkFacade}
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com) created on 13/04/2018
 */
public class DtmfSinkFacadeProvider {

    private static final AtomicInteger detectorId = new AtomicInteger(0);

    private PriorityQueueScheduler scheduler;
    private DtmfDetectorProvider detectorProvider;

    public DtmfSinkFacadeProvider(PriorityQueueScheduler scheduler, DtmfDetectorProvider detectorProvider) {
        this.scheduler = scheduler;
        this.detectorProvider = detectorProvider;
    }

    public DtmfSinkFacade provide() {
        AudioOutput output = new AudioOutput(scheduler, ComponentType.DTMF_DETECTOR.getType());
        OOBOutput oobOutput = new OOBOutput(scheduler, ComponentType.DTMF_DETECTOR.getType());
        int id = detectorId.getAndIncrement();
        InbandDtmfSink inbandSink = new InbandDtmfSink("inband-dtmf-sink-" + id, detectorProvider.provide(), output);
        Rfc2833DtmfSink oobSink = new Rfc2833DtmfSink("oob-dtmf-sink-" + id, new Rfc2833DtmfDetector(500), oobOutput);
        return new DtmfSinkFacade(inbandSink, oobSink);
    }

}
