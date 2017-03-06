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

package org.restcomm.media.control.mgcp.pkg.ann;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.control.mgcp.controller.signal.Signal;

/**
 * Implements play announcement signal.
 * 
 * @author yulian oifa
 */
public class Play extends Signal implements PlayerListener {

	private Text oc = new Text("oc");
	private Text of = new Text("of");

	private Player player;
	private String uri;

	private final static Logger logger = Logger.getLogger(Play.class);

	public Play(String name) {
		super(name);
	}

	@Override
	public void execute() {
		logger.info("Executing...");

		player = this.getPlayer();

		try {
			player.addListener(this);

			// get options of the request
			this.uri = getTrigger().getParams().toString();

			player.setURL(uri);
			logger.info("Assigned url " + player);
		} catch (TooManyListenersException e) {
			this.sendEvent(of);
			this.complete();
			logger.error("OPERATION FAILURE", e);
			return;
		} catch (MalformedURLException e) {
			logger.info("Received URL in invalid format , firing of");
			this.sendEvent(of);
			this.complete();
			return;
		} catch (ResourceUnavailableException e) {
			logger.info("Received URL can not be found , firing of");
			this.sendEvent(of);
			this.complete();
			return;
		}

		player.activate();
	}

	@Override
	public boolean doAccept(Text event) {
		if (event.equals(oc)) {
			return true;
		}

		if (event.equals(of)) {
			return true;
		}

		return false;
	}

	@Override
	public void cancel() {
		terminate();
	}

	private Player getPlayer() {
		return (Player) getEndpoint().getResource(MediaType.AUDIO, ComponentType.PLAYER);
	}

	private void terminate() {
		if (player != null) {
			player.removeListener(this);
			player.deactivate();
			player = null;
		}
	}

	@Override
	public void reset() {
		super.reset();
		terminate();
	}

	@Override
	public void process(PlayerEvent event) {
		switch (event.getID()) {
		case PlayerEvent.STOP:
			terminate();
			this.sendEvent(oc);
			this.complete();
			break;
		case PlayerEvent.FAILED:
			terminate();
			this.sendEvent(of);
			this.complete();
			break;
		}
	}
}
