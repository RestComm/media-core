package org.mobicents.media.io.ice.network.nio;

import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
