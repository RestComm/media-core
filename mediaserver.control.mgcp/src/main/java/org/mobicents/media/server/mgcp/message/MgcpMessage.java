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

package org.mobicents.media.server.mgcp.message;

import java.nio.ByteBuffer;

/**
 * Represents MGCP message.
 * 
 * @author yulian oifa
 */
public abstract class MgcpMessage {
    public final static int CREATE_CONNECTION = 1;
    public final static int MODIFY_CONNECTION = 2;
    public final static int DELETE_CONNECTION = 3;
    public final static int REQUEST_NOTIFICATION = 4;
    public final static int AUDIT_CONNECTION = 5;
    public final static int AUDIT_ENDPOINT = 6;
    public final static int NOTIFY = 7;
    
    
    //backing data buffer;
    private byte[] buff = new byte[8192];
    
    public MgcpMessage() {
    }
        
    public int getType() {
        return 0;
    }
    
    public abstract int getTxID();
    
    public void read(ByteBuffer buffer) {
        buffer.get(buff, 0, buffer.limit());
        parse(buff, 0, buffer.limit());
    }
    
    protected abstract void parse(byte[] buff, int offset, int len);
    
    public void write(ByteBuffer buffer) {
        
    }
    
    /**
     * Clean parameters
     */
    public abstract void clean();
}
