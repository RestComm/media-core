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

package org.restcomm.media.ice.network.stun;

import org.restcomm.media.ice.IceAgent;
import org.restcomm.media.network.deprecated.server.NioServer;

/**
 * Non-blocking server to listen to STUN connectivity checks that happen as part
 * of the ICE handshake.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectivityCheckServer extends NioServer {

	private final IceAgent agent;
	private final StunHandler stunHandler;
	private final StunListener stunListener;

	public ConnectivityCheckServer(IceAgent agent) {
		super();
		this.agent = agent;
		this.stunHandler = new StunHandler(this.agent);
		this.stunListener = new StunListenerImpl(this.agent);
		this.stunHandler.addListener(this.stunListener);
		super.addPacketHandler(this.stunHandler);
	}

	protected class StunListenerImpl implements StunListener {

		private final IceAgent agent;

		public StunListenerImpl(IceAgent agent) {
			this.agent = agent;
		}

		@Override
		public void onBinding(BindingSuccessEvent event) {
			// Tell the ICE agent to select a candidate for the correct media
			// stream and component
			this.agent.selectCandidatePair(ConnectivityCheckServer.this.currentChannel);
		}
		
	}

}
