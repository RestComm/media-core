package org.mobicents.media.core.ice.network;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Pipeline that automatically moves to the next available component as soon as
 * the current component expires.
 * 
 * @author Henrique Rosa
 * 
 * @param <T>
 *            The type of expiring components that compose the pipeline
 */
public class ExpiringPipeline<T extends Expirable> implements Pipeline<T> {

	private final List<T> components;
	private volatile int currentIndex;

	public ExpiringPipeline(Collection<T> components) {
		this.components = new LinkedList<T>(components);
		this.currentIndex = -1;
	}

	public ExpiringPipeline() {
		this.components = new LinkedList<T>();
		this.currentIndex = -1;
	}

	public T getCurrent() {
		T current = null;
		if (this.currentIndex >= 0) {
			synchronized (this.components) {
				// Get current component
				current = this.components.get(this.currentIndex);
				// If current component has expired, move to next available
				if (current.isExpired()) {
					if (hasNext()) {
						current = move();
					} else {
						// All components have expired
						// Pipeline finished execution
						current = null;
					}
				}
			}
		}
		return current;
	}

	public boolean hasNext() {
		boolean exists = false;
		synchronized (this.components) {
			if (this.currentIndex < this.components.size() - 1) {
				exists = true;
			}
		}
		return exists;
	}

	public boolean hasPrevious() {
		boolean exists = false;
		synchronized (this.components) {
			if (this.currentIndex > 0) {
				exists = true;
			}
		}
		return exists;
	}

	private T move() {
		if (!hasNext()) {
			throw new IllegalStateException("Pipeline has no more components.");
		}

		T next = null;
		synchronized (this.components) {
			this.currentIndex++;
			next = this.components.get(this.currentIndex);
		}
		return next;
	}

	public void add(T element) {
		synchronized (components) {
			this.components.add(element);
			// If pipeline is empty, elect this element as 'current'
			if (this.currentIndex == -1) {
				this.currentIndex = 0;
			}
		}
	}
}
