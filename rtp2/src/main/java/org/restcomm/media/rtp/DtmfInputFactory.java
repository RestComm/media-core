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

package org.restcomm.media.rtp;

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.component.oob.OOBInput;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DtmfInputFactory {

    static final String NAME_PREFIX = "dtmf-input";
    static final int MIN_ID = 0;
    static final int MAX_ID = 1000000;

    // Core Components
    private final PriorityQueueScheduler scheduler;
    private final Clock wallClock;

    // DTMF Input Factory
    private final AtomicInteger idGenerator;

    public DtmfInputFactory(PriorityQueueScheduler scheduler, Clock wallClock) {
        super();
        this.scheduler = scheduler;
        this.wallClock = wallClock;
        this.idGenerator = new AtomicInteger(MIN_ID);
    }

    public DtmfInput build() {
        // Build dependencies
        int id = generateId();
        String name = NAME_PREFIX + "-" + id;
        RtpClock rtpClock = new RtpClock(this.wallClock);
        OOBInput oobInput = new OOBInput(id);

        // Build component
        DtmfInput dtmfInput = new DtmfInput(name, scheduler, rtpClock, oobInput);
        return dtmfInput;
    }

    private int generateId() {
        int nextId = this.idGenerator.incrementAndGet();
        this.idGenerator.compareAndSet(MAX_ID, MIN_ID);
        return nextId;
    }

}
