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

import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Implements compound oob mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class OOBMixer {
	// scheduler for mixer job scheduling
	private PriorityQueueScheduler scheduler;

	// The pool of components
	private ConcurrentMap<OOBComponent> components = new ConcurrentMap<OOBComponent>();

	Iterator<OOBComponent> activeComponents;

	private MixTask mixer;
	private volatile boolean started = false;

	public long mixCount = 0;

	// gain value
	private double gain = 1.0;

	public OOBMixer(PriorityQueueScheduler scheduler) {
		this.scheduler = scheduler;
		this.mixer = new MixTask();
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
	
	public boolean isStarted() {
        return started;
    }

	public void start() {
		mixCount = 0;
		started = true;
		scheduler.submit(mixer, PriorityQueueScheduler.MIXER_MIX_QUEUE);
	}

	public void stop() {
		started = false;
		mixer.cancel();
	}

	private class MixTask extends Task {
		int sourceComponent = 0;
		private Frame current;

		public MixTask() {
			super();
		}

		@Override
		public int getQueueNumber() {
			return PriorityQueueScheduler.MIXER_MIX_QUEUE;
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
				scheduler.submit(this, PriorityQueueScheduler.MIXER_MIX_QUEUE);
				mixCount++;
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
			mixCount++;
			return 0;
		}
	}
}
