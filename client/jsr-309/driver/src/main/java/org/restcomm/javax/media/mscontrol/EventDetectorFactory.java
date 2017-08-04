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
package org.restcomm.javax.media.mscontrol;

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import javax.media.mscontrol.MediaEvent;

public abstract class EventDetectorFactory {

	protected String pkgName = null;
	protected String eventName = null;
	protected boolean isOnEndpoint = false;
	protected boolean isSuccessful = true;
	
	public EventDetectorFactory(String pkgName, String eventName, boolean isOnEndpoint, boolean isSuccessful) {		
		this.pkgName = pkgName;
		this.eventName = eventName;
		this.isOnEndpoint = isOnEndpoint;
		this.isSuccessful = isSuccessful;
	}

	public abstract MediaEvent generateMediaEvent();

	public EventName generateMgcpEvent(String params, ConnectionIdentifier connId) {
		if (this.isOnEndpoint) {
			if (params != null) {
				return new EventName(PackageName.factory(pkgName), MgcpEvent.factory(eventName).withParm(params));
			} else {
				return new EventName(PackageName.factory(pkgName), MgcpEvent.factory(eventName));
			}
		} else {
			if (params != null) {
				return new EventName(PackageName.factory(pkgName), MgcpEvent.factory(eventName).withParm(params),
						connId);
			} else {
				return new EventName(PackageName.factory(pkgName), MgcpEvent.factory(eventName), connId);
			}
		}
	}

	public String getPkgName() {
		return pkgName;
	}

	public String getEventName() {
		return eventName;
	}

	public boolean isOnEndpoint() {
		return isOnEndpoint;
	}
	
	@Override
	public String toString() {
		return "EventDetectorFactory[pkg=" + pkgName + "][event=" + eventName + "][endpoint=" + isOnEndpoint + "]";
	}

}
