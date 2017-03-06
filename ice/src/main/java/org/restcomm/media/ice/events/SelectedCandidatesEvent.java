/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.ice.events;

import java.net.SocketAddress;

import org.restcomm.media.ice.IceAgent;

/**
 * Event that must be fired when an ICE Agent finishes selecting all candidate pairs.
 * 
 * @author Henrique Rosa
 * 
 */
public class SelectedCandidatesEvent {

    @Deprecated
    private IceAgent source;

    private SocketAddress remotePeer;

    public SelectedCandidatesEvent(SocketAddress remotePeer) {
        this.remotePeer = remotePeer;
    }

    @Deprecated
    public SelectedCandidatesEvent(IceAgent source) {
        this.source = source;
    }

    @Deprecated
    public IceAgent getSource() {
        return source;
    }

    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

}
