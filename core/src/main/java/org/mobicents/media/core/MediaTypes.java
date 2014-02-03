package org.mobicents.media.core;

/**
 * Enumeration of Media Types supported by the Media Server.<br>
 * Should also be used to create media streams on ICE agents, using standard
 * names.
 * 
 * @author Henrique Rosa
 * 
 */
public enum MediaTypes {
	AUDIO, VIDEO, APPLICATION;

	public String lowerName() {
		return name().toLowerCase();
	}
}
