package org.mobicents.javax.media.mscontrol.mediagroup;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.RecorderEvent;
import javax.media.mscontrol.resource.Trigger;

/**
 * 
 * @author amit bhayani
 *
 */
public class RecorderEventImpl implements RecorderEvent {
	private Recorder recorder = null;
	private EventType eventType = null;
	private Qualifier qualifier = null;
	private Trigger rtcTrigger = null;
	private int duration = -1;

	private String errorText = null;
	private MediaErr error = MediaErr.NO_ERROR;

	private boolean isSuccessful = false;
	
	public RecorderEventImpl( EventType eventType){
		this.eventType = eventType;
	}

	public RecorderEventImpl(Recorder recorder, EventType eventType, boolean isSuccessful) {
		this(eventType);
		this.recorder = recorder;		
		this.isSuccessful = isSuccessful;
	}

	public RecorderEventImpl(Recorder recorder, EventType eventType, boolean isSuccessful, MediaErr error,
			String errorText) {
		this(recorder, eventType, isSuccessful);
		this.errorText = errorText;
		this.error = error;

	}

	public RecorderEventImpl(Recorder recorder, EventType eventType, boolean isSuccessful, Qualifier qualifier,
			Trigger rtcTrigger, int duration) {
		this(recorder, eventType, isSuccessful);

		this.qualifier = qualifier;
		this.rtcTrigger = rtcTrigger;
		this.duration = duration;
	}

	public int getDuration() {
		return this.duration;
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

	public Recorder getSource() {
		return this.recorder;
	}

	public boolean isSuccessful() {
		return this.isSuccessful;
	}
	
	public void setSuccessful(boolean isSuccessful){
		this.isSuccessful = isSuccessful;
	}

}
