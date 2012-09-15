package org.mobicents.javax.media.mscontrol;

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

public abstract class EventGeneratorFactory {

	protected String pkgName = null;
	protected String eventName = null;
	protected boolean isOnEndpoint = false;

	public EventGeneratorFactory(String pkgName, String eventName, boolean isOnEndpoint) {
		this.pkgName = pkgName;
		this.eventName = eventName;
		this.isOnEndpoint = isOnEndpoint;
	}

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
		return "EventGeneratorFactory[pkg=" + pkgName + "][event=" + eventName + "][endpoint=" + isOnEndpoint + "]";
	}
}
