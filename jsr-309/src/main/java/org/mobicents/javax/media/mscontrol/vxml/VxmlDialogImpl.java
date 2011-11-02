package org.mobicents.javax.media.mscontrol.vxml;


import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.vxml.VxmlDialog;
import javax.media.mscontrol.vxml.VxmlDialogEvent;

import org.apache.log4j.Logger;
import org.mobicents.javax.media.mscontrol.container.ContainerImpl;
import org.mobicents.javax.media.mscontrol.MediaSessionImpl;

/**
 * 
 * @author amit bhayani
 * 
 */
public class VxmlDialogImpl extends ContainerImpl implements VxmlDialog {
	public static Logger logger = Logger.getLogger(VxmlDialogImpl.class);

	private URI uri = null;
	private Parameters parameters = null;

	public VxmlDialogImpl(MediaSessionImpl mediaSession, Parameters params)
			throws MsControlException {
		super(mediaSession, null);
		this.session = mediaSession;
		this.maxJoinees = 1;
		this.parameters = params;

		String userDefId = null;
		if (this.parameters != null) {
			userDefId = (this.parameters.get(MEDIAOBJECT_ID)).toString();
		}

	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	public void acceptEvent(String paramString, Map<String, Object> paramMap) {
		// TODO Auto-generated method stub

	}

	public void prepare(URL paramURL, Parameters paramParameters, Map<String, Object> paramMap) {
		// TODO Auto-generated method stub

	}

	public void prepare(String paramString, Parameters paramParameters, Map<String, Object> paramMap) {
		// TODO Auto-generated method stub

	}

	public void start(Map<String, Object> paramMap) {
		// TODO Auto-generated method stub

	}

	public void terminate(boolean paramBoolean) {
		// TODO Auto-generated method stub

	}

	public Joinable[] getJoinables() {
		// TODO Auto-generated method stub
		return null;
	}

	public Iterator<MediaObject> getMediaObjects() {
		// TODO Auto-generated method stub
		return null;
	}

	public <T extends MediaObject> Iterator<T> getMediaObjects(Class<T> paramClass) {
		// TODO Auto-generated method stub
		return null;
	}


	public void release() {
		// TODO Auto-generated method stub

	}


	public void addListener(MediaEventListener<VxmlDialogEvent> paramMediaEventListener) {
		// TODO Auto-generated method stub

	}

	public void removeListener(MediaEventListener<VxmlDialogEvent> paramMediaEventListener) {
		// TODO Auto-generated method stub

	}

}
