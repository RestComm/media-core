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

package org.mobicents.media.server.spi.memory;

import java.util.ArrayList;

/**
 *
 * @author kulikov
 */
public class Partition {

    protected int size;
    private ArrayList<Frame> heap = new ArrayList();

    protected Partition(int size) {
        this.size = size;
    }
    
    protected synchronized Frame allocate() {
//        if (true) return new Frame(this, new byte[size]);
        if (heap.isEmpty()) {
            return new Frame(this, new byte[size]);
        }
        return heap.remove(0);
    }

    protected synchronized void recycle(Frame frame) {
        frame.setHeader(null);
        frame.setDuration(Long.MAX_VALUE);
        frame.setEOM(false);
        heap.add(frame);
        //queue.offer(frame, frame.getDelay(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

}
