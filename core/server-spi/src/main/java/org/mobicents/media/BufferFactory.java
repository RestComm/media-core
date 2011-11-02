/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media;

import java.util.ArrayList;

public class BufferFactory {

    private int BUFF_SIZE = 8192;
    private ArrayList<Buffer> list = new ArrayList<Buffer>();
    private int size;

    public BufferFactory(int size) {
        this.size = size;
        init();
    }    
    
    public BufferFactory(int size, int buffSize) {
        this.size = size;
        this.BUFF_SIZE = buffSize;
        init();
    }

    private void init() {
        for (int i = 0; i < size; i++) {
            Buffer buffer = new Buffer();
            buffer.setFactory(this);
            list.add(buffer);
        }
    }
    public Buffer allocate() {
        Buffer buffer = null;
        if (!list.isEmpty()) {
            buffer = list.remove(0);
        }

        if (buffer != null) {
            return buffer;
        }

        buffer = new Buffer();
        buffer.setFactory(this);

        return buffer;
    }

    public void deallocate(Buffer buffer) {
        if (list.size() < size && buffer != null ) {
            buffer.setDiscard(false);
            buffer.setHeader(null);
            list.add(buffer);
        }
    }
}
