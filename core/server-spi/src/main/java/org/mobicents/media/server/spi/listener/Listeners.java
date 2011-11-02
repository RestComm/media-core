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
     */
    public void dispatch(Event event) {
        for (int i = 0; i < list.length; i++) {
            if (list[i] != null) list[i].process(event);
        }
    }
}
