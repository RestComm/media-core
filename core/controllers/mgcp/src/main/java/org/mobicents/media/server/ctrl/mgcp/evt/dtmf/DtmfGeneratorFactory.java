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
package org.mobicents.media.server.ctrl.mgcp.evt.dtmf;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

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
import org.mobicents.media.server.spi.resource.DtmfDetector;

/**
 * 
 * @author amit bhayani
 * @author baranowb
 */
public class DtmfGeneratorFactory implements GeneratorFactory {

	private String name;
	private MgcpPackage mgcpPackage;

	
	//FIXME: why this is ignored ?
	//private int eventID;
    private String digit;
        
	public String getEventName() {
		return this.name;
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

    public String getDigit() {
        return digit;
    }

    public void setDigit(String digit) {
        this.digit = digit;
    }

    
	public SignalGenerator getInstance(MgcpController controller, String param) {
		return new MgcpDtmfGenerator(digit);
	}

	/**
	 * 
	 * @author amit bhayani
	 * @author baranowb
	 */
	private class MgcpDtmfGenerator extends SignalGenerator {

		private String digit = null;

		private org.mobicents.media.server.spi.resource.DtmfGenerator dtmfGenerator = null;

		private MediaType mediaType;

		private Class detectorInterface;

		private Class generatorInterface;

		public MgcpDtmfGenerator(String digit) {
			super(digit);
			this.digit = digit;
			this.mediaType = MediaType.AUDIO;
			this.detectorInterface = DtmfDetector.class;
			this.generatorInterface = org.mobicents.media.server.spi.resource.DtmfGenerator.class;
		}

		@Override
		public void cancel() {
			// Do nothing
		}

		@Override
		protected boolean doVerify(Connection connection) {
			MediaSource source = (MediaSource) connection.getComponent(MediaType.AUDIO,this.generatorInterface);
			if(source!=null)
			{
				dtmfGenerator = (org.mobicents.media.server.spi.resource.DtmfGenerator) source.getInterface(this.generatorInterface);
				return true;
			}else
			{
				return false;
			}
			
		}

		@Override
		protected boolean doVerify(Endpoint endpoint) {
			MediaSource source = endpoint.getSource(MediaType.AUDIO);
			if(source!=null)
			{
				this.dtmfGenerator = source.getInterface(org.mobicents.media.server.spi.resource.DtmfGenerator.class);
				return this.dtmfGenerator!=null;
			}else
			{
				return false;
			}
			
		}

		@Override
		public void start(Request request) {
			dtmfGenerator.setDigit(this.digit);
			dtmfGenerator.start();
		}

		@Override
		public void configureDetector(EventDetector det) {
			//actually dtmf detector should take care of it.
			det.setMediaType(MediaType.AUDIO);
			det.setDetectorInterface(this.detectorInterface);
			
		}

	    public EventName getSignalRequest(){
	    	MgcpEvent mgcpEvent = MgcpEvent.factory(name);
	    	
//	    	if(this.params != null){
//	    		mgcpEvent = mgcpEvent.withParm(this.params);
//	    	}
	    	
	    	 if (this.connectionIdentifier != null) {
	             return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent, connectionIdentifier);
	         }
	         return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent);
	    }		

	}

}
