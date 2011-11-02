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
package org.mobicents.media.server.ctrl.mgcp.evt;

import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;

import java.util.List;

import org.mobicents.media.server.ctrl.mgcp.MgcpController;
import org.mobicents.media.server.spi.MediaType;

/**
 * 
 * @author kulikov
 * @author baranowb
 */
public class MgcpPackage {

	private String name;
	private int id;
	private MgcpController controller;

	//private List<GeneratorFactory> generators;
	//private List<DetectorFactory> detectors;
	private GeneratorFactory[] generators;
	private DetectorFactory[] detectors;
	
	private MediaType mediaType;
	//this will be used for packages which have defined interface - like "D"
	private Class detectorInterface;

	
	/**
	 * @return the mediaType
	 */
	public MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * @return the detectorInterface
	 */
	public String getDetectorInterface() {
		if(this.detectorInterface==null)
		{
			return null;
		}
		return detectorInterface.toString();
	}
	public Class getCDetectorInterface() {
		
		return detectorInterface;
	}
	/**
	 * @param mediaType the mediaType to set
	 */
	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	/**
	 * @param detectorInterface the detectorInterface to set
	 */
	public void setDetectorInterface(String detectorInterface) {
		
		try {
			this.detectorInterface = Class.forName(detectorInterface);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			//e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MgcpController getController() {
		return controller;
	}

	public void setController(MgcpController controller) {
		this.controller = controller;
	}

	public GeneratorFactory[] _getGenerators() {
		return generators;
	}

	public void setGenerators(List<GeneratorFactory> signals) {
		
		try {
			if (signals != null) {
				this.generators = new GeneratorFactory[signals.size()];
				for (int i=0;i<signals.size();i++) {
					GeneratorFactory factory =  signals.get(i);
					factory.setPackage(this);
					this.generators[i] = factory;
				}
			}else
			{
				this.generators = new GeneratorFactory[]{};
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public DetectorFactory[] _getDetectors() {
		return detectors;
	}

	public void setDetectors(List<DetectorFactory> events) {
		
		try {
			if (events != null) {
				this.detectors = new DetectorFactory[events.size()];
				for (int i=0;i<events.size();i++) {
					DetectorFactory factory =  events.get(i);
					factory.setPackage(this);
					this.detectors[i] = factory;
				}
			}else
			{
				this.detectors = new DetectorFactory[]{};
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SignalGenerator getGenerator(MgcpEvent evt) {
		if (generators != null) {
			for (GeneratorFactory factory : generators) {
				if (factory.getEventName().equals(evt.getName())) {
					return factory.getInstance(controller, evt.getParms());
				}
			}
		}
		return null;
	}

	public EventDetector getDetector(MgcpEvent evt, RequestedAction[] actions) {
		if (detectors != null) {
			for (DetectorFactory factory : detectors) {
				if (factory.getEventName().equals(evt.getName())) {
					EventDetector det = null;
					det = factory.getInstance(evt.getParms(), actions,this.detectorInterface,this.mediaType);
					det.setPackage(this);
					return det;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MgcpPackage other = (MgcpPackage) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MgcpPackage [detectorInterface=" + detectorInterface + ", id="
				+ id + ", mediaType=" + mediaType + ", name=" + name + "]";
	}
	
	
}
