package org.mobicents.javax.media.mscontrol;

import javax.media.mscontrol.MediaEvent;
import javax.media.mscontrol.MediaEventListener;

/**
 * 
 * @author amit bhayani
 *
 */
public class EventExecutor implements Runnable {
	private MediaEventListener mediaEventListener = null;
	private MediaEvent mediaEvent = null;

	public EventExecutor(MediaEventListener mediaEventListener, MediaEvent mediaEvent) {
		this.mediaEventListener = mediaEventListener;
		this.mediaEvent = mediaEvent;
	}

	public void run() {
		this.mediaEventListener.onEvent(this.mediaEvent);
	}

}
