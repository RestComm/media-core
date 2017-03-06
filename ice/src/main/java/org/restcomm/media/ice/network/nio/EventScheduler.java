/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.ice.network.nio;

import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EventScheduler implements Runnable {

	private List<ScheduledEvent> events;
	private final NioServer server;
	private volatile boolean running;

	public EventScheduler(NioServer server) {
		this.events = new ArrayList<ScheduledEvent>();
		this.server = server;
		this.running = false;
	}

	public void schedule(DatagramChannel channel, byte[] data, int dataLength) {
		byte[] dataCopy = Arrays.copyOf(data, dataLength);
		synchronized (this.events) {
			ScheduledEvent event = new ScheduledEvent(this.server, channel, dataCopy);
			this.events.add(event);
			this.events.notify();
		}
	}

	@Override
	public void run() {
		this.running = true;
		ScheduledEvent event;

		// Wait for data to become available
		while (this.running && !hasWork()) {
			synchronized (this.events) {
				while (this.events.isEmpty()) {
					try {
						this.events.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// Incoming event received
				event = this.events.remove(0);
			}
			// Launch the event on the server
			event.launch();
		}
		// cleanup remaining event after thread stops
		cleanup();
	}
	
	private boolean hasWork() {
		synchronized (this.events) {
			return !this.events.isEmpty();
		}
	}

	public void stop() {
		this.running = false;
	}
	
	public void stopNow() {
		this.running = false;
		cleanup();
	}

	private void cleanup() {
		synchronized (this.events) {
			this.events.clear();
		}
	}

}
