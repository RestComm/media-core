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
package org.mobicents.media.server.mgcp.controller.naming;

import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.utils.Text;

import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;

/**
 * Naming tree for MGCP endpoints.
 * 
 * @author kulikov
 */
public class NamingTree {
    //the root of the tree
    private NamingNode<EndpointQueue> root = new NamingNode(new Text("root"), null);
    private ConcurrentCyclicFIFO<Text[]> patterns = new ConcurrentCyclicFIFO();
    
    //exceptions
    private static UnknownEndpointException UNKNOWN_ENDPOINT_EXCEPTION;// = new UnknownEndpointException();
    
    public NamingTree() {
    	//preloading text arrays
    	for(int j=0;j<50;j++)
    	{
    		Text[] pattern = new Text[10];
    		for (int i = 0; i < pattern.length; i++) {
    			pattern[i] = new Text();
    		}
    		
    		patterns.offer(pattern);
    	}
    }
    
    /**
     * Adds endpoint to this tree.
     * 
     * @param endpoint the endpoint to be added.
     */
    public synchronized void register(MgcpEndpoint endpoint,EndpointInstaller installer) {    	
    	//split name into tokens
    	String[] tokens = endpoint.getName().split("/");
        
    	//create text identifier for each token
    	Text[] path = new Text[tokens.length];
    	for (int i = 0; i < tokens.length; i++) {
    		path[i] = new Text(tokens[i]);
    	}
        
    	//create tree of nodes starting from root
    	NamingNode currentNode = root;
    	for (int i = 0; i < path.length - 1; i++) {
    		//try to find node first
    		NamingNode foundNode = currentNode.find(new Text[]{path[i]}, 1);            
            
    		//create new node if not found
    		currentNode = foundNode != null ? foundNode : currentNode.createChild(path[i]);            
            
    		//attach endpoint queue if it is not attached yet
    		if (currentNode.poll() == null) {
    			currentNode.attach(new EndpointQueue());
    		}
    	}
        
    	//finally add endpoint
    	((EndpointQueue)currentNode.poll()).add(endpoint);
    	((EndpointQueue)currentNode.poll()).setInstaller(installer);
    }
    
    /**
     * Removes endpoint from this tree.
     * 
     * @param endpoint the endpoint to be removed.
     */
    public void unregister(MgcpEndpoint endpoint) {    	
    }
    
    /**
     * Searches endpoints with specified name pattern
     * 
     * @param name the name pattern to search
     * @param endpoints the list of matching endpoints
     * @return number of found endpoints
     */
    public int find(Text name, MgcpEndpoint[] endpoints) throws UnknownEndpointException {
    	Text[] pattern=patterns.poll();
    	if(pattern==null)
    	{
    		//adding more
    		pattern = new Text[10];
    		for (int i = 0; i < pattern.length; i++) {
    			pattern[i] = new Text();
    		}
    	}
    	
    	//clean prev search
    	NamingNode node = null;
    	int n = name.divide('/', pattern);        
    	
    	//search node
    	node = root.find(pattern, n - 1);
    	
    	if (node == null) {
    		throw new UnknownEndpointException();
    	}
        
    	int result=((EndpointQueue)node.poll()).find(pattern[n - 1], endpoints);
    	patterns.offer(pattern);
    	return result;
    }
}
