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

package org.restcomm.media.ice.lite;

import org.restcomm.media.ice.IceAgent;
import org.restcomm.media.ice.network.stun.ConnectivityCheckServer;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceLiteAgent extends IceAgent {

	public IceLiteAgent() {
		super();
	}

	@Override
	public boolean isLite() {
		return true;
	}

	@Override
	public boolean isControlling() {
		return false;
	}

	@Override
	public void start() {
		if (this.running) {
			throw new IllegalStateException("The ICE agent is already started.");
		}
		
		// Candidates must be gathered and a selector available
		if (this.selector == null) {
			throw new IllegalStateException("Cannot start agent without gathering candidates first.");
		}
		// Initialize connectivity server if necessary
		if (this.connectivityCheckServer == null) {
			this.connectivityCheckServer = new ConnectivityCheckServer(this);
		}
		// Run connectivity check server
		this.connectivityCheckServer.start(this.selector);
		this.running = true;
	}

	@Override
	public void stop() {
		if (this.running) {
			this.running = false;
			// Stop the connectivity check server
			this.connectivityCheckServer.stop();
		}
	}
}
