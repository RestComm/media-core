/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.core.naming;

import java.util.ArrayList;

import org.mobicents.media.core.endpoints.BaseEndpointImpl;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Implements naming server for media server.
 *
 * The name of the endpoint has heirarhical structure and endpoints are
 * stored in tree structure. The service allows register and unregister
 * endpoints with this service and as well allows to lookup the endpoint using
 * the name of endpoint in two styles: exclusive and non-exclusive
 *
 * The exclusive lookup is used when endpoint name is wildcarded and returned
 * endpoint is not visible for future lookups. The user must explicit share
 * the endpoint after usage to make visible again.
 *
 * @author yulian oifa
 */
public class NamingService {

    //service entry point
    private Node root = new Node(new String[]{""}, 0);

    private final Object LOCK = new Object();
    /**
     * Registers endpoint.
     * After registration the endpoint can be looked up using its name.
     *
     * @param endpoint the endpoint to be registered.
     */
    public void register(Endpoint endpoint) {
    	synchronized(LOCK) {
    		EndpointName name = new EndpointName(endpoint.getLocalName());
    		Node node = getNode(name.getCategory(), true);
    		node.queue.add(endpoint);
    	}
    }

    /**
     * Unregisters endpoint.
     * Unregistered endpoint is not longer visible for lookups.
     *
     * @param endpoint the endpoint to be unregistered.
     */
    public void unregister(Endpoint endpoint) {
    	synchronized(LOCK) {
    		EndpointName name = new EndpointName(endpoint.getLocalName());
    		Node node = getNode(name.getCategory(), true);
    		node.queue.remove(endpoint);
    	}
    }

    /**
     * Gets the endpoint with specified name.
     *
     * @param name the name of the endpoint to lookup
     * @param exclusive the mode of search. if true the returned endpoint won't
     * be visible for future lookups unit it will be explicitly shared.     *
     * @return the endpoint with specified name
     * @throws ResourceUnavailableException if name is wildcarded and no free endpoints now
     * @throws UnknownEndpointException the name of endpoint is unknown.
     */
    public Endpoint lookup(String name, boolean exclusive) throws ResourceUnavailableException, UnknownEndpointException {
    	synchronized(LOCK) {
    		EndpointName endpointName = new EndpointName(name);
    		Node node = getNode(endpointName.getCategory(), false);
    		if (node == null) return null;
    		return node.lookup(endpointName.getID(), exclusive);
    	}
    }

    /**
     * Makes endpoint visible for lookups if it was looked up exclusively before
     * @param endpoint the endpoint to share.
     */
    public void share(Endpoint endpoint) {
    	synchronized(LOCK) {
    		((BaseEndpointImpl) endpoint).setState(EndpointState.READY);
    	}
    }

    /**
     * Gets the node holding the endpoint.
     *
     * @param fqn1 the fully qualified name of the node
     * @param allowNew allows create node if it is not exist yet
     * @return the node with specified name.
     */
    private Node getNode(String fqn1, boolean allowNew) {
        String[] fqn = fqn1.split("/");
        return root.getNode(fqn, 1, allowNew);
    }
}

class Node {
    private String name;

    private ArrayList<Node> childs = new ArrayList<Node>();
    protected ArrayList<Endpoint> queue = new ArrayList<Endpoint>();

    protected Node(String[] fqn, int k) {
        this.name = fqn[k];
    }
    
    protected Node getNode(String[] fqn, int k, boolean allowNew) {
        if (k == fqn.length) return this;
        
        for (int i = 0; i < childs.size(); i++) {
            if (childs.get(i).name.equals(fqn[k])) {
                if (k == fqn.length - 1) {
                    return childs.get(i);
                } else {
                    return childs.get(i).getNode(fqn, k + 1, allowNew);
                }
            }
        }

        if (allowNew) {
            Node node = new Node(fqn, k);
            childs.add(node);
            return node.getNode(fqn, k + 1, allowNew);
        }

        return null;
    }

    protected Endpoint lookup(String id, boolean exclusive) throws ResourceUnavailableException, UnknownEndpointException {
        return id.equals("$") ? lookupAny(exclusive) : lookupConcrete(id);
    }

    private Endpoint lookupConcrete(String id) throws UnknownEndpointException {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getLocalName().endsWith(id)) return queue.get(i);
        }
        throw new UnknownEndpointException();
    }

    private Endpoint lookupAny(boolean exclusive) throws ResourceUnavailableException {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getState() == EndpointState.READY) {
                Endpoint endpoint = queue.remove(i);
                queue.add(endpoint);
                return endpoint;
            }
        }
        throw new ResourceUnavailableException();
    }
}