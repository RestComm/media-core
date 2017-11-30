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
package org.restcomm.javax.media.mscontrol.mediagroup.signals;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.resource.Trigger;

/**
 * 
 * @author amit bhayani
 * 
 */
public class SignalDetectorEventImpl implements SignalDetectorEvent {

	private SignalDetector detector = null;
	private EventType eventType = null;
	private Qualifier qualifier = null;
	private Trigger rtcTrigger = null;

	private String errorText = null;
	private MediaErr error = MediaErr.NO_ERROR;

	private int patterIndex = -1;

	private String signal = null;

	private boolean isSuccessful = false;

	public SignalDetectorEventImpl(EventType eventType) {
		this.eventType = eventType;
	}

	public SignalDetectorEventImpl(SignalDetector detector, EventType eventType, boolean isSuccessful) {
		this(eventType);
		this.detector = detector;
		this.isSuccessful = isSuccessful;
	}

	public SignalDetectorEventImpl(SignalDetector detector, EventType eventType, boolean isSuccessful, MediaErr error,
			String errorText) {
		this(detector, eventType, isSuccessful);
		this.errorText = errorText;
		this.error = error;
	}

	public SignalDetectorEventImpl(SignalDetector detector, EventType eventType, boolean isSuccessful, String signal) {
		this(detector, eventType, isSuccessful);
		this.signal = signal;
	}

	public SignalDetectorEventImpl(SignalDetector detector, EventType eventType, boolean isSuccessful, Qualifier qualifier) {
		this(detector, eventType, isSuccessful);
		this.qualifier = qualifier;
	}
        
	public SignalDetectorEventImpl(SignalDetector detector, EventType eventType, boolean isSuccessful, String signal,
			int patterIndex, Qualifier qualifier, Trigger rtcTrigger) {
		this(detector, eventType, isSuccessful, signal);
		this.patterIndex = patterIndex;
		this.qualifier = qualifier;
		this.rtcTrigger = rtcTrigger;
	}

	public int getPatternIndex() {
		return this.patterIndex;
	}

	public Value[] getSignalBuffer() {
		return null;
	}

	public String getSignalString() {
		return this.signal;
	}

	public Qualifier getQualifier() {
		return this.qualifier;
	}

	public Trigger getRTCTrigger() {
		return this.rtcTrigger;
	}

	public MediaErr getError() {
		return this.error;
	}

	public String getErrorText() {
		return this.errorText;
	}

	public EventType getEventType() {
		return this.eventType;
	}

	public SignalDetector getSource() {
		return this.detector;
	}

	public boolean isSuccessful() {
		return this.isSuccessful;
	}

	public void setDetector(SignalDetector detector) {
		this.detector = detector;
	}

	public void setQualifier(Qualifier qualifier) {
		this.qualifier = qualifier;
	}

	protected void setRtcTrigger(Trigger rtcTrigger) {
		this.rtcTrigger = rtcTrigger;
	}

	protected void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	protected void setError(MediaErr error) {
		this.error = error;
	}

	public void setPatterIndex(int patterIndex) {
		this.patterIndex = patterIndex;
	}

	public void setSignal(String signal) {
		this.signal = signal;
	}

	public void setSuccessful(boolean isSuccessful) {
		this.isSuccessful = isSuccessful;
	}
        
    @Override
        public String toString() {
            return String.format("%s(%s, signal=%s)", eventType, qualifier, this.signal);
        }

}
