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

package org.restcomm.media.resource.player.video.mpeg;

import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author kulikov
 */
public class FullBox extends Box {

    /** is an integer that specifies the version of this format of the box. */
    private int version;
    /** is a map of flags */
    private int flags;
    
    public FullBox(long size, String type) {
        super(size, type);
    }

    /**
     * Gets the version of the format of the box.
     * 
     * @return the integer format identifier.
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * Gets the map of flags.
     * 
     * @return the indeteger where loweresrt 24 bits are map of flags.
     */
    public int getFlags() {
        return flags;
    }
    
    protected long read64(DataInputStream fin) throws IOException {
        return (fin.readInt() << 32) | fin.readInt();
    }
    
    @Override
    protected int load(DataInputStream fin) throws IOException {
        this.version = fin.readByte();
        this.flags = this.read24(fin);
        return 4;
    }
    
}
