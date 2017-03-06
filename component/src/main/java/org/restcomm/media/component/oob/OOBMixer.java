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

package org.restcomm.media.component.oob;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.restcomm.media.concurrent.ConcurrentMap;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.memory.Frame;

/**
 * Implements compound oob mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class OOBMixer {

	private final PriorityQueueScheduler scheduler;
	private final ConcurrentMap<OOBComponent> components;
	private final MixTask mixer;

	private final AtomicBoolean started;
	private final AtomicLong mixCount;

	public OOBMixer(PriorityQueueScheduler scheduler) {
		this.scheduler = scheduler;
		this.components = new ConcurrentMap<OOBComponent>();
		this.mixer = new MixTask();
		this.started = new AtomicBoolean(false);
		this.mixCount = new AtomicLong(0);
	}
	
	public long getMixCount() {
        return mixCount.get();
    }

	public void addComponent(OOBComponent component) {
		components.put(component.getComponentId(), component);
	}

	/**
	 * Releases unused input stream
	 * 
	 * @param input
	 *            the input stream previously created
	 */
	public void release(OOBComponent component) {
		components.remove(component.getComponentId());
	}

    public void start() {
        if (!this.started.get()) {
            started.set(true);
            mixCount.set(0);
            scheduler.submit(mixer, PriorityQueueScheduler.MIXER_MIX_QUEUE);
        }
    }

    public void stop() {
        if (this.started.get()) {
            started.set(false);
            mixer.cancel();
        }
    }

	private final class MixTask extends Task {

		public MixTask() {
			super();
		}

		@Override
		public int getQueueNumber() {
			return PriorityQueueScheduler.MIXER_MIX_QUEUE;
		}

		@Override
		public long perform() {
		    int sourceComponent = 0;
	        Frame current = null;
		    
			// summarize all
	        Iterator<OOBComponent> activeComponents = components.valuesIterator();
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
				scheduler.submit(this, PriorityQueueScheduler.MIXER_MIX_QUEUE);
				mixCount.incrementAndGet();
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
			scheduler.submit(this, PriorityQueueScheduler.MIXER_MIX_QUEUE);
			mixCount.incrementAndGet();
			return 0;
		}
	}
}
