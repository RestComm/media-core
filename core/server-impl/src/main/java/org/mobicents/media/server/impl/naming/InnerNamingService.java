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
package org.mobicents.media.server.impl.naming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import org.apache.log4j.Logger;
import org.jboss.beans.metadata.api.annotations.Install;
import org.jboss.beans.metadata.api.annotations.Uninstall;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.NamingService;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * 
 * @author kulikov
 */
public class InnerNamingService implements NamingService {

    private FastList endps = new FastList();
    private FastList categories = new FastList();
    
    private HashMap<String, Endpoint> endpoints = new HashMap<String, Endpoint>();
    
    private List<Endpoint> tempEndpoints = new ArrayList<Endpoint>();
    
    private int endpointCount = 0;
    private final static Logger logger = Logger.getLogger(InnerNamingService.class);

    public void start() {
        logger.info("Started");
    }

    
    public void stop() {
        logger.info("Stopped");
        Collection<Endpoint> list = endpoints.values();
        for (Endpoint endpoint : list) {
            endpoint.stop();
            endpointCount = 0;
            logger.info("Stopped endpoint: local name = " + endpoint.getLocalName());
        }
    }

    @Install
    public void addEndpoint(Endpoint endpoint) throws ResourceUnavailableException {
        endpoints.put(endpoint.getLocalName(), endpoint);
        endpointCount++;
    }

    @Uninstall
    public void removeEndpoint(Endpoint endpoint) {
        endpoints.remove(endpoint.getLocalName());
        endpointCount--;
        logger.info("Unregistered endpoint: local name " + endpoint.getLocalName());
    }

    public Endpoint lookup(String endpointName) throws ResourceUnavailableException {
        if (endpointName.endsWith("$")) {
            return findAny(endpointName); // findAny(endpointName);
        } else {
            return find(endpointName, true);
        }
    }

    public Endpoint lookup(String endpointName, boolean allowInUse) throws ResourceUnavailableException {
        if (endpointName.endsWith("$")) {
            return findAny(endpointName); // findAny(endpointName);
        } else {
            return find(endpointName, allowInUse);
        }
    // return null;
    }
    
    public Endpoint[] lookupall(String endpointName) throws ResourceUnavailableException {
        if (endpointName.endsWith("*")) {
            return findAll(endpointName); // findAny(endpointName);
        } else {
            return null;
        }
    // return null;
    } 
    
    public synchronized Endpoint[] findAll(String name) throws ResourceUnavailableException {
        Endpoint[] endpts = null;
        String prefix = name.substring(0, name.indexOf("*") - 1);
        Set<String> keys = endpoints.keySet();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
            	tempEndpoints.add(endpoints.get(key));
            }
        } // end of for
        
        if(tempEndpoints.size() > 0){
        	endpts = new Endpoint[tempEndpoints.size()];
        	endpts = tempEndpoints.toArray(endpts);
        	tempEndpoints.clear();
        	return endpts;
        } else 
        
        return null;
    }    

    public synchronized Endpoint findAny(String name) throws ResourceUnavailableException {
        int count = 0;
       Collection<Endpoint> list = endpoints.values();
       for (Endpoint e : list) {
           if (e.getState() == EndpointState.READY) {
               count++;
           }
       }
       logger.info("-------------Free endpoints =" + count);
        // TODO : Can name have two '$'? In this case the search will be
        // slow once we add logic for this

        Endpoint endpt = null;
        String prefix = name.substring(0, name.indexOf("$") - 1);
        String suffix = null;
        if (name.indexOf("$") + 1 > name.length()) {
            suffix = name.substring(name.indexOf("$") + 1, name.length());
        }

        Set<String> keys = endpoints.keySet();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                if (suffix != null) {
                    if (key.contains(suffix)) {
                        endpt = endpoints.get(key);
                        if (endpt.getState() == EndpointState.READY) {
                            //endpt.setInUse(true);
                            return endpt;
                        }
                    }
                } else {
                    endpt = endpoints.get(key);
                    if (endpt.getState() == EndpointState.READY) {
//                        endpt.setInUse(true);
                        return endpt;
                    }
                }
            }
        } // end of for

        if (endpt == null || endpt.getState() != EndpointState.READY) {
            throw new ResourceUnavailableException("No Endpoint found for " + name);
        }

        return endpt;

    }

    public synchronized Endpoint find(String name, boolean allowInUse) throws ResourceUnavailableException {
        Endpoint endpt = endpoints.get(name);
        if (endpt == null) {
            throw new ResourceUnavailableException("No Endpoint found for " + name);
        }
        if (endpt.getState() == EndpointState.BUSY && !allowInUse) {
            throw new ResourceUnavailableException("Endpoint " + name + " is in use");
        } else {
//            endpt.setInUse(true);
            return endpt;
        }
    }

    protected Collection<String> getNames(Collection<String> prefixes, NameToken token, Iterator<NameToken> tokens) {
        ArrayList<String> list = new ArrayList();
        if (!tokens.hasNext()) {
            while (token.hasMore()) {
                String s = token.next();
                for (String prefix : prefixes) {
                    list.add(prefix + "/" + s);
                }
            }
            return list;
        } else {
            Collection<String> newPrefixes = new ArrayList();
            while (token.hasMore()) {
                String s = token.next();
                for (String prefix : prefixes) {
                    newPrefixes.add(prefix + "/" + s);
                }
            }
            return getNames(newPrefixes, tokens.next(), tokens);
        }
    }

    public int getEndpointCount() {
        return this.endpointCount;
    }
}
