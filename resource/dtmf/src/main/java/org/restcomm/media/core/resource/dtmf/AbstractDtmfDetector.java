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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract DTMF detector class - implements observer logic.
 *
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 */
public abstract class AbstractDtmfDetector implements DtmfDetector {

    private static final Logger logger = LogManager.getLogger(AbstractDtmfDetector.class);

    private Set<DtmfEventObserver> observers = ConcurrentHashMap.newKeySet();

    @Override
    public void notify(DtmfEvent event) {
        // Inform observers about DTMF tone detection
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
