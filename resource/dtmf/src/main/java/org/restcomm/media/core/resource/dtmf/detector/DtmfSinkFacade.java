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

package org.restcomm.media.core.resource.dtmf.detector;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.component.audio.AudioOutput;
import org.restcomm.media.core.component.oob.OOBOutput;

/**
 * DTMF sink with in-band and out-of-band DTMF detector components
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class DtmfSinkFacade implements DtmfEventObserver, DtmfEventSubject {

    private static final long serialVersionUID = 450306501541827622L;
    private static final Logger logger = LogManager.getLogger(DtmfSinkFacade.class);

    private final InbandDtmfSink inbandSink;
    private final Rfc2833DtmfSink oobSink;

    private Set<DtmfEventObserver> observers = ConcurrentHashMap.newKeySet();

    public DtmfSinkFacade(InbandDtmfSink inbandSink, Rfc2833DtmfSink oobSink) {
        this.inbandSink = inbandSink;
        this.inbandSink.observe(this);

        this.oobSink = oobSink;
        this.oobSink.observe(this);
    }

    public AudioOutput getInbandOutput() {
        return this.inbandSink.getOutput();
    }

    public OOBOutput getOutbandOutput() {
        return this.oobSink.getOutput();
    }
    
    public void activate() {
        this.inbandSink.activate();
        this.oobSink.activate();
    }

    public void deactivate() {
        this.inbandSink.deactivate();
        this.oobSink.deactivate();
    }

    @Override
    public void onDtmfEvent(DtmfEvent event) {
        // Propagate DTMF Event to registered observers
        notify(event);
    }

    @Override
    public void notify(DtmfEvent event) {
        for (DtmfEventObserver observer : observers) {
            observer.onDtmfEvent(event);
        }
    }

    @Override
    public void observe(DtmfEventObserver observer) {
       final boolean added = this.observers.add(observer);
        if (added && logger.isDebugEnabled()) {
            logger.debug("Registered observer DtmfEventObserver@" + observer.hashCode() + ". Count: " + observers.size());
        }
    }

    @Override
    public void forget(DtmfEventObserver observer) {
        final boolean removed = observers.remove(observer);
        if (removed && logger.isDebugEnabled()) {
            logger.debug("Unregistered observer DtmfEventObserver@" + observer.hashCode() + ". Count: " + observers.size());
        }
    }

}
