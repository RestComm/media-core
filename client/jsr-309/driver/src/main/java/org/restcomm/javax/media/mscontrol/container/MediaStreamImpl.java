/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol.container;

import java.io.Serializable;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.join.JoinableContainer;
import javax.media.mscontrol.join.JoinableStream;

/**
 *
 * @author kulikov
 */
public class MediaStreamImpl implements JoinableStream {
    private StreamType type;
    private ContainerImpl container;
    
    protected MediaStreamImpl(ContainerImpl container, StreamType type) {
        this.container = container;
        this.type = type;
    }
    
    public StreamType getType() {
        return type;
    }

    public JoinableContainer getContainer() {
        return container;
    }

    public Joinable[] getJoinees(Direction arg0) throws MsControlException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Joinable[] getJoinees() throws MsControlException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void join(Direction arg0, Joinable arg1) throws MsControlException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unjoin(Joinable arg0) throws MsControlException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2) throws MsControlException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unjoinInitiate(Joinable arg0, Serializable arg1) throws MsControlException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
