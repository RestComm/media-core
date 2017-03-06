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

package org.restcomm.media.spi.listener;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.lang.InterruptedException;

/**
 * Implements collection of listener.
 * 
 * Allows concurrently modify list.
 * 
 * @author yulian oifa
 */
public class Listeners<L extends Listener> {    
    //list of registered listeners
    private ArrayList<Listener> list=new ArrayList<Listener>();
    private ArrayList<Listener> processingList=new ArrayList<Listener>();
    private Semaphore accessSemaphore=new Semaphore(1);    
    
    /**
     * Creates list of listeners with default size of 10.
     */
    public Listeners() {        
    }
        
    /**
     * Adds listener to the collection.
     * 
     * @param listener the listener to be added
     * @throws org.restcomm.media.spi.listener.TooManyListenersException
     */    
    public void add(L listener) throws TooManyListenersException {
    	try {
    		accessSemaphore.acquire();
    	}
    	catch(InterruptedException e) {
    		
    	}
    	
    	list.add(listener);
    	accessSemaphore.release();
    }
    
    /**
     * Removes listener from the collection.
     * @param listener the listener to be removed.
     */
    public void remove(L listener) {
    	try {
    		accessSemaphore.acquire();
    	}
    	catch(InterruptedException e) {
    		
    	}
    	
    	list.remove(listener);
    	accessSemaphore.release();
    }
    
    /**
     * Removes all listeners.
     */
    public void clear() {
    	try {
    		accessSemaphore.acquire();
    	}
    	catch(InterruptedException e) {
    		
    	}
    	
    	list.clear();
    	accessSemaphore.release();
    }
    
    /**
     * Dispatched event to all registered listeners.
     * 
     * @param event the event to be dispatched
     * @return true if event was delivered at least to one listener
     */
    public boolean dispatch(Event event) {
    	try {
    		accessSemaphore.acquire();
    	}
    	catch(InterruptedException e) {
    		
    	}
    	    	
    	processingList.clear();
    	processingList.addAll(list);
    	accessSemaphore.release();        	
    	boolean res= (processingList.size()!=0);        
    	
    	for(int i=0;i<processingList.size();i++)
    		processingList.get(i).process(event);        
    	    
    	return res;
    }    
}
