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

import java.util.ArrayList;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.utils.Text;

/**
 * Storage for endpoints of same type with search and reordering functions.
 * 
 * @author kulikov
 */
public class EndpointQueue {
    //reserved space for endpoint queue
    private final static int SIZE = 100;
    
    //wildcards
    private final static Text ANY = new Text("$");
    private final static Text ALL = new Text("*");
    
    //queue of endpoints
    private ArrayList<Holder> queue = new ArrayList(SIZE);
    
    //reference for just found endpoind
    private Holder holder;
    //index
    private int k;
    
    /**
     * Adds new endpoint to the queue.
     * 
     * @param endpoint the endpoint to be added
     */
    public void add(MgcpEndpoint endpoint) {
        queue.add(0, new Holder(endpoint));
    }
    
    /**
     * Removes endpoint from the queue.
     * 
     * @param endpoint the endpoint to be removed.
     */
    public void remove(MgcpEndpoint endpoint) {
        holder = null;
        for (Holder h : queue) {
            if (h.endpoint == endpoint) {
                holder = h;
                break;
            }
        }
        
        if (holder != null) {
            queue.remove(holder);
        }
    }
     
    /**
     * Finds endpoints matching to name pattern.
     * 
     * If "any" endpoint was requested, then first matching endpoint will be locked
     * and not available for search in future until this endpoint will be explicitly unlocked.
     * 
     * @param name the name pattern for search
     * @param endpoints collection which will be filled by found endpoints
     * @return the number of found endpoints.
     */
    public int find(Text name, MgcpEndpoint[] endpoints) {
        //return all endpoint if all requested
        if (name.equals(ALL)) {
            k = 0;
            for (Holder h : queue) {
                endpoints[k++] = h.endpoint;
            }
            return queue.size();
        }
        
        //return first free if ANY endpoint requested
        if (name.equals(ANY)) {
            //clean results from prev search
            holder = null;
            for (Holder h : queue) {
                if (h.endpoint.getState() == MgcpEndpoint.STATE_FREE) {
                    holder = h;
                    break;
                }
            }
            
            //prepare result if found
            if (holder != null) {
                //lock it first
                holder.endpoint.lock();
                
                //move to the tail
                //this will speed up search process 
                queue.remove(holder);
                queue.add(holder);
                
                endpoints[0] = holder.endpoint;
                return 1;
            }
    
            return 0;
        }
        
        //search for exact matching
        for (Holder h : queue) {
            if (h.name.equals(name)) {
                endpoints[0] = h.endpoint;
                return 1;
            }
        }
        
        return 0;
    }
    
    private class Holder {
        protected MgcpEndpoint endpoint;
        protected Text name;
        
        protected Holder(MgcpEndpoint endpoint) {
            this.endpoint = endpoint;
            String[] tokens = endpoint.getName().split("/");
            this.name = new Text(tokens[tokens.length - 1]);
        }
    }
}
