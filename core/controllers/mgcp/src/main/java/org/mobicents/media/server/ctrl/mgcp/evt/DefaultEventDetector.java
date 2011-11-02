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
package org.mobicents.media.server.ctrl.mgcp.evt;

import jain.protocol.ip.mgcp.message.parms.RequestedAction;

import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 * 
 * @author kulikov
 */
public class DefaultEventDetector extends EventDetector {

	// private ActionNotify actionNotify;

	public DefaultEventDetector(MgcpPackage pkgName, String eventName, int eventID,
			String params, RequestedAction[] actions) {
		super(pkgName, eventName, eventID, params, actions);
	}

	public DefaultEventDetector(MgcpPackage mgcpPackage, String eventName,
			int eventID, String params,
			RequestedAction[] actions, Class interface1, MediaType type) {
		this(mgcpPackage, eventName, eventID, params, actions);
		super._interface = interface1;
		super.mediaType = type;
	}

	@Override
	public void performAction(NotifyEvent event, RequestedAction action) {

		// this check is not required since each component/resoruce gets
		// dedicated detector!
		// if (!event.getSource().getName().matches(this.getResourceName())) {
		// return;
		// }

		if (event.getEventID() != this.getEventID()) {
			return;
		}

		// @TODO implement action selector
		getRequest().sendNotify(this.getEventName());

	}

}
