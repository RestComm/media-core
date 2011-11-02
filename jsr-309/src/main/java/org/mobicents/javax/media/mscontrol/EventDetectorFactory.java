package org.mobicents.javax.media.mscontrol;

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
