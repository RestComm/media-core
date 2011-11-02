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
package org.mobicents.media.server.ctrl.mgcp.evt.ivr;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.util.HashSet;
import java.util.Set;

import org.mobicents.media.Component;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.ctrl.mgcp.MgcpController;
import org.mobicents.media.server.ctrl.mgcp.Request;
import org.mobicents.media.server.ctrl.mgcp.evt.EventDetector;
import org.mobicents.media.server.ctrl.mgcp.evt.GeneratorFactory;
import org.mobicents.media.server.ctrl.mgcp.evt.MgcpPackage;
import org.mobicents.media.server.ctrl.mgcp.evt.SignalGenerator;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.resource.Recorder;

/**
 * 
 * @author amit bhayani
 * @author baranowb
 */
public class RecorderFactory implements GeneratorFactory {

	private String name;
	private MgcpPackage mgcpPackage;

	public String getEventName() {
		return this.name;
	}
	
	public SignalGenerator getInstance(MgcpController controller, String parms) {
		// ??
		MediaType type = this.mgcpPackage.getMediaType();
		if(type == null)
		{
			//its audio than
			type = MediaType.AUDIO;
		}
		return new MgcpRecorder(parms,type);
	}

	

	public MgcpPackage getPackage() {
		return this.mgcpPackage;
	}

	public void setEventName(String eventName) {
		this.name = eventName;
	}

	public void setPackage(MgcpPackage mgcpPackage) {
		this.mgcpPackage = mgcpPackage;
	}
	
	private static String getExtension(String parms) {
		int dotIndex = parms.lastIndexOf(".");
		
		return parms.substring(dotIndex+1);
	}
	
	private class MgcpRecorder extends SignalGenerator {

		private Recorder recorder;
		private String url;
		private Class detectorInterface;
		private Class generatorInterface;
		private MediaType mediaType;

		public MgcpRecorder(String url, MediaType mediaType) {
			super(url);
			this.url = url;
			this.detectorInterface = Recorder.class;
			this.generatorInterface = this.detectorInterface;
			this.mediaType = mediaType;
		}

		@Override
		public void cancel() {
			this.recorder.stop();
		}

		@Override
		protected boolean doVerify(Connection connection) {
			//MediaSource source = (MediaSource) connection.getComponent(this.mediaType,this.generatorInterface);
			Component source = (Component) connection.getComponent(this.mediaType,this.generatorInterface);
			if(source!=null)
			{
				recorder =  source.getInterface(Recorder.class);
				return true;
			}else
			{
				return false;
			}
			
		}

		@Override
		protected boolean doVerify(Endpoint endpoint) {
			//MediaSource source = endpoint.getSource(this.mediaType);
			Component source = endpoint.getSource(this.mediaType);
			if(source!=null)
			{
				this.recorder = source.getInterface(Recorder.class);
				return this.recorder!=null;
			}else
			{
				return false;
			}
			
		}

		@Override
		public void start(Request request) {
	            try {
	            recorder.setRecordFile(url);
	            recorder.start();
	            } catch (Exception e) {
	                //@FIXME allow method to throw excetion
	                e.printStackTrace();
	            }
		}

		@Override
		public void configureDetector(EventDetector det) {
			det.setDetectorInterface(this.detectorInterface);
			det.setMediaType(this.mediaType);
			
		}
		
	    public EventName getSignalRequest(){
	    	MgcpEvent mgcpEvent = MgcpEvent.factory(name);
	    	
	    	if(this.params != null){
	    		mgcpEvent = mgcpEvent.withParm(this.params);
	    	}
	    	
	    	 if (this.connectionIdentifier != null) {
	             return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent, connectionIdentifier);
	         }
	         return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent);
	    }
	}
}
