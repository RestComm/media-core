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

package org.mobicents.media.server.spi.listener;

/**
 * Implements collection of listener.
 * 
 * Allows concurrently modify list.
 * 
 * @author kulikov
 */
public class Listeners<L extends Listener> {
    
    //the size of array reserved for listeners
    private int size = 10;
    //list of registered listeners
    private Listener[] list;

    /**
     * Creates list of listeners with default size of 10.
     */
    public Listeners() {
        list = new Listener[size];
    }
    
    /**
     * Creates list of listeners with specified size.
     * 
     * @param  size the size of list reserved for list.
     */
    public Listeners(int size) {
        this.size = size;
        list = new Listener[size];
    }
    
    /**
     * Adds listener to the collection.
     * 
     * @param listener the listener to be added
     * @throws org.mobicents.media.server.spi.listener.TooManyListenersException
     */
    public void add(L listener) throws TooManyListenersException {
        boolean res = false;
        
        for (int i = 0; i < list.length; i++) {
            if (list[i] != null) continue;
            list[i] = listener;
            res = true;
            break;
        }
        
        if (!res) throw new TooManyListenersException();
    }
    
    /**
     * Removes listener from the collection.
     * @param listener the listener to be removed.
     */
    public void remove(Listener listener) {
        for (int i = 0; i < list.length; i++) {
            if (list[i] == listener) list[i] = null;
        }
    }
    
    /**
     * Removes all listeners.
     */
    public void clear() {
        for (int i = 0; i < list.length; i++) {
            list[i] = null;
        }
    }
    
    /**
     * Dispatched event to all registered listeners.
     * 
     * @param event the event to be dispatched
     * @return true if event was delivered at least to one listener
     */
    public boolean dispatch(Event event) {
        boolean res = false;
        
        for (int i = 0; i < list.length; i++) {            
            if (list[i] != null) {
                list[i].process(event);
                res = true;
            }
        }
        
        return res;
    }
    
}
