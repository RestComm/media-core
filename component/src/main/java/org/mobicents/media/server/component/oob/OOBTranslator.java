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

package org.mobicents.media.server.component.oob;

import java.util.Iterator;

import org.mobicents.media.server.component.OobRelay;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Implements compound oob mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class OOBTranslator implements OobRelay {

    // The pool of components
    private ConcurrentMap<OOBComponent> components = new ConcurrentMap<OOBComponent>();
    Iterator<OOBComponent> activeComponents;

    // Mixing job
    private Scheduler scheduler;
    private TranslateTask translator;
    private volatile boolean started = false;
    private long executionCount = 0;

    public OOBTranslator(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.translator = new TranslateTask();
    }

    public long getExecutionCount() {
        return executionCount;
    }

    @Override
    public void addComponent(OOBComponent component) {
        components.put(component.getComponentId(), component);
    }

    @Override
    public void removeComponent(OOBComponent component) {
        components.remove(component.getComponentId());
    }

    @Override
    public void start() {
        if (!started) {
            started = true;
            executionCount = 0;
            scheduler.submit(translator, Scheduler.MIXER_MIX_QUEUE);
        }
    }

    @Override
    public void stop() {
        if (started) {
            started = false;
            translator.cancel();
        }
    }

    private class TranslateTask extends Task {
        int sourceComponent = 0;
        private Frame current;

        public TranslateTask() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return Scheduler.MIXER_MIX_QUEUE;
        }

        @Override
        public long perform() {
            // summarize all
            activeComponents = components.valuesIterator();
            while (activeComponents.hasNext()) {
                OOBComponent component = activeComponents.next();
                component.perform();
                current = component.getData();
                if (current != null) {
                    sourceComponent = component.getComponentId();
                    break;
                }
            }

            if (current == null) {
                scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                executionCount++;
                return 0;
            }

            // get data for each component
            activeComponents = components.valuesIterator();
            while (activeComponents.hasNext()) {
                OOBComponent component = activeComponents.next();
                if (component.getComponentId() != sourceComponent) {
                    component.offer(current.clone());
                }
            }

            current.recycle();
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            executionCount++;
            return 0;
        }
    }
}
