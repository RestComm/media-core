package org.restcomm.javax.media.mscontrol;

public class DefaultEventGeneratorFactory extends EventGeneratorFactory {

	public DefaultEventGeneratorFactory(String pkgName, String eventName, boolean isOnEndpoint) {
		super(pkgName, eventName, isOnEndpoint);		
	}
	
	@Override
	public String toString() {
		return super.toString() + "  DefaultEventGeneratorFactory";
	}

}
