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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.connection;

import java.io.IOException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class MyTestConnection extends BaseConnection {

    private volatile boolean created;
    private volatile boolean opened;
    private volatile boolean closed;
    private volatile boolean failed;

    public MyTestConnection(String id, Connections connections) throws Exception {
        super(id, connections,false);
    }

    public void setOtherParty(Connection other) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOtherParty(byte[] descriptor) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getPacketsReceived(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getBytesReceived(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getBytesReceived() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getPacketsTransmitted(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getBytesTransmitted(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getBytesTransmitted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getJitter(MediaType media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getJitter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onCreated() throws Exception {
        this.created = true;
    }

    public boolean isCreated() {
        return this.created;
    }

    @Override
    protected void onFailed() {
    }


    @Override
    protected void onOpened() throws Exception {
        this.opened = true;
    }

    public boolean isOpened() {
        return this.opened;
    }

    @Override
    protected void onClosed() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void setOtherParty(Text descriptor) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
