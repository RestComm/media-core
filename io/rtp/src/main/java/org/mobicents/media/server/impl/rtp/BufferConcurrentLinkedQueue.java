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

package org.mobicents.media.server.impl.rtp;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Start time:11:42:12 2009-05-17<br>
 * Project: mobicents-media-server-core<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class BufferConcurrentLinkedQueue<T> extends ConcurrentLinkedQueue<T> {

	private T stored = null;

	/**
	 * 	
	 */
	public BufferConcurrentLinkedQueue() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * tttttt
	 * 
	 * @param c
	 */
	public BufferConcurrentLinkedQueue(Collection<T> c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	@Override
	public T peek() {
		if (this.stored != null) {
			return stored;
		} else {
			return super.peek();
		}
	}

	@Override
	public T poll() {
		if (this.stored != null) {
			try {
				T b= stored;
				return b;
			} finally {
				stored = null;
			}
		} else {
			return super.poll();
		}
	}

	@Override
	public int size() {
		if (this.stored != null) {

			return super.size() + 1;

		} else {
			return super.size();
		}
	}
	
	public boolean storeAtHead(T toStore)
	{
		if(this.stored == null)
		{
			this.stored = toStore;
			return true;
		}else
		{
			return false;
		}
	}

}
