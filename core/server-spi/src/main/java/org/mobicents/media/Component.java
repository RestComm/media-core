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

package org.mobicents.media;

import java.io.Serializable;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.NotificationListener;

/**
 * <i>Component</i> is an Object that is responsible for any media 
 * data processing. 
 * 
 * Examples of components are the audio player, recoder, 
 * DTMF detector, etc. The <code>Component</code> is a supper class for all 
 * media processing components.
 * 
 * @author kulikov
 */
public interface Component extends Serializable {
    
    /**
     * Gets the unique identifier of this component.
     * 
     * @return
     */
    public String getId();
    
    /**
     * Gets the name of the component.
     * The component of same type can share same name.
     * 
     * @return name of this component;
     */
    public String getName();
    
    /**
     * Gets the format used for communication.
     * 
     * @return the Format object.
     */
    public Format getFormat();
    
    /**
     * Gets the reference to endpoint to which this component belongs
     * 
     * @return the endpoint reference.
     */
    public Endpoint getEndpoint();
    
    /**
     * Sets reference to the endpoint to which this component belongs.
     * 
     * @param endpoint the reference to the endpoint.
     */
    public void setEndpoint(Endpoint endpoint);
    
    /**
     * Reference to the connection to which this component belongs.
     * 
     * @return the connection instance.
     */    
    public Connection getConnection();
    
    /**
     * Set reference to the connection to which this component belongs.
     * 
     * @param connection the connection instance.
     */
    public void setConnection(Connection connection);
    
    /**
     * Registers new notfications listener 
     * 
     * @param listener the listener object.
     */
    public void addListener(NotificationListener listener);
    
    /**
     * Unregisters new notfications listener 
     * 
     * @param listener the listener object.
     */
    public void removeListener(NotificationListener listener);
    
    /**
     * Resets any transmission stats.
     */
    public void resetStats();
    
    /**
	 * This method returns proper interface. Returns concrete object if
	 * implementing this interface if this sink supports interface contract. In
	 * general case it may return <b>this</b>
	 * 
	 * @param <T>
	 * @param interfaceType
	 * @return
	 */
	public <T> T getInterface(Class<T> interfaceType);
}
