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
