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
package org.mobicents.media.server.spi;

import java.io.Serializable;

import java.util.Collection;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;

/**
 * The basic implementation of the endpoint.
 *
 * An Endpoint is a logical representation of a physical entity, such as an
 * analog phone or a channel in a trunk. Endpoints are sources or sinks of data
 * and can be physical or virtual. Physical endpoint creation requires hardware
 * installation while software is sufficient for creating a virtual Endpoint.
 * An interface on a gateway that terminates a trunk connected to a PSTN switch
 * is an example of a physical Endpoint. An audio source in an audio-content
 * server is an example of a virtual Endpoint.
 *
 * @author Oleg Kulikov.
 * @author amit.bhayani
 */
public interface Endpoint extends Serializable {
    
    /**
     * Gets the local name attribute.
     *
     * @return the local name.
     */
    public String getLocalName();

    /**
     * Gets the current state of the endpoint
     * 
     * @return the state of the endpoint.
     */
    public EndpointState getState();
    
    /**
     * Gets the list of supported media types.
     * 
     * @return collection of media types.
     */
    public Collection<MediaType> getMediaTypes();
    
    /**
     * Starts endpoint.
     */
    public void start() throws ResourceUnavailableException ;
    
    /**
     * Terminates endpoint's execution.
     */
    public void stop();
    
    /**
     * Creates new connection with specified mode.
     *
     * @param mode the constant which identifies mode of the connection to be created.
     */
    public Connection createConnection()
            throws TooManyConnectionsException, ResourceUnavailableException;

    /**
     * Creates new connection with specified mode.
     *
     * @param mode the constant which identifies mode of the connection to be created.
     */
    public Connection createLocalConnection()
            throws TooManyConnectionsException, ResourceUnavailableException;

    /**
     * Deletes specified connection.
     *
     * @param connectionID the identifier of the connection to be deleted.
     */
    public void deleteConnection(String connectionID);

    /**
     * Deletes all connection associated with this Endpoint.
     */
    public void deleteAllConnections();

    /**
     * Register NotificationListener to listen for <code>MsNotifyEvent</code>
     * which are fired due to events detected by Endpoints like DTMF. Use above
     * execute methods to register for event passing appropriate
     * <code>RequestedEvent</code>
     * 
     * @param listener
     */
    public void addNotificationListener(NotificationListener listener);

    /**
     * Remove the NotificationListener
     * 
     * @param listener
     */
    public void removeNotificationListener(NotificationListener listener);

    
    public MediaSink getSink(MediaType media) ;

    public MediaSource getSource(MediaType media);
    
    /**
     * Generates media description for the specified media type.
     * 
     * @param mediaType the media type for which descriptor is requested.
     * @return SDP media descriptor 
     * @throws org.mobicents.media.server.spi.ResourceUnavailableException
     */
    public String describe(MediaType mediaType) throws ResourceUnavailableException;
    
    /**
     * Return time when it was freed last time.
     * @return number of milliseconds
     */
    public long lastTimeUsed();
}
