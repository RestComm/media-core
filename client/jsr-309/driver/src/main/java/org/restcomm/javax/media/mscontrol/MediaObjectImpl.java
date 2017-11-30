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
package org.restcomm.javax.media.mscontrol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;

import org.restcomm.javax.media.mscontrol.spi.DriverImpl;

/**
 *
 * @author kulikov
 */
public abstract class MediaObjectImpl implements MediaObject {
    public final static Parameter ENDPOINT_NAME = ParameterImpl.create("ENDPOINT_NAME");
    
    private static int GENERATOR = 0;
    private URI uri;
    
    private MediaObjectImpl parent;
    private DriverImpl driver;
    
    private Parameters parameters = new ParametersImpl();
    
    private ArrayList<String> IDs = new ArrayList();
    private String objectID;
      
    public MediaObjectImpl(MediaObjectImpl parent, DriverImpl driver, Parameters parameters) throws MsControlException {
        this.parent = parent;
        this.driver = driver;
        
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
        
        //generating unique uri
        String prefix = parent != null ? parent.getURI().toString() : 
            "mscontrol://" + driver.getRemoteDomainName();
        
        String objectName = getClass().getSimpleName();
        if (objectName.endsWith("Impl")) {
            objectName = objectName.substring(0, objectName.length() - 4);
        }

        objectID = null;
        if (parameters != null) {
            objectID = (String)parameters.get(MEDIAOBJECT_ID);
        }
        
        //if not specified generate unique;
        if (objectID == null) {
            objectID = objectName + (++GENERATOR);
        }
        
        if (!Character.isLetterOrDigit(objectID.charAt(0)) && objectID.charAt(0) != '/') {
            throw new MsControlException("Illegal MEDIAOBJECT_ID: " + objectID);
        }
        
        if (parent != null && parent.IDs.contains(objectID)) {
            throw new MsControlException("MEDIAOBJECT_ID must be unique: " + objectID);
        }
        
        String localName = null;
        if (parameters != null) {
            localName = (String)parameters.get(ENDPOINT_NAME);
        }

        if (localName == null) {
            localName = objectID;
        }

        try {
            uri = new URI(prefix + "/" + objectID);
        } catch (URISyntaxException e) {
        }
        
        if (parent != null) {
            parent.IDs.add(objectID);
        }
    }
    
    public URI getURI() {
        return uri;
    }

    public DriverImpl getDriver() {
        return driver;
    }
    
    public void setParameters(Parameters parameters) {
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }

    public Parameters getParameters(Parameter[] list) {
        Parameters params = new ParametersImpl();
        for (Parameter p : list) {
            params.put(p, parameters.get(p));
        }
        return params;
    }

    public Parameters createParameters() {
        return new ParametersImpl();
    }

    public void info(String s) {
        if (parent != null) {
            driver.info(String.format("%s %s %s", parent.objectID, objectID, s));
        } else {
            driver.info(String.format("%s %s", objectID, s));
        }
    }

    public void debug(String s) {
        if (parent != null) {
            driver.debug(String.format("%s %s %s", parent.objectID, objectID, s));
        } else {
            driver.debug(String.format("%s %s", objectID, s));
        }
    }

    public void warn(String s) {
        if (parent != null) {
            driver.warn(String.format("%s %s %s", parent.objectID, objectID, s));
        } else {
            driver.warn(String.format("%s %s", objectID, s));
        }
    }
    
    public String getObjectID() {
        return this.objectID;
    }
    
    @Override
    public String toString() {
        return objectID;
    }
}
