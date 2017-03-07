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
