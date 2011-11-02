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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.naming.InnerNamingService;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointFactory;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author kulikov
 */
public class EndpointManager {
    private ArrayList<EndpointFactory> endpoints = new ArrayList();
        
    private InnerNamingService namingService;
    
    private Logger logger = Logger.getLogger(EndpointManager.class);
    
    public EndpointManager() {
    }
    
    public void setNamingService(InnerNamingService namingService) throws ResourceUnavailableException {
        this.namingService = namingService;
    }
    
    
    public void activate() throws ResourceUnavailableException {
        for (EndpointFactory factory : endpoints) {
            Collection<Endpoint> list = factory.install();
            for (Endpoint e : list) {
                e.start();
                logger.info("Started endpoint " + e.getLocalName());
                namingService.addEndpoint(e);
            }
        }
    }
    
    public void addEndpoint(EndpointFactory endpoint) throws ResourceUnavailableException {
        endpoints.add(endpoint);
        logger.info("Installed Endpoint Factory : " + endpoint);
    }
    
    public void removeEndpoint(EndpointFactory endpoint) {
    }
    
}
