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

package org.mobicents.media.server.ctrl.mgcp.signal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Holds definition of signals and loads signal upon request.
 * 
 * @author kulikov
 */
public class Signals {
    //the map between event name and class implementing 
    private static HashMap<String, String> events = new HashMap();
    //the map between signal name and class implementing 
    private static HashMap<String, String> signals = new HashMap();
     
    private Logger logger = Logger.getLogger(Signals.class);
    
    static {
        signals.put("T/test1", "org.mobicents.media.server.ctrl.mgcp.pkg.innertest.TestSignal");
        signals.put("T/test2", "org.mobicents.media.server.ctrl.mgcp.pkg.innertest.TestSignal2");
        signals.put("T/test3", "org.mobicents.media.server.ctrl.mgcp.pkg.innertest.TestSignal2");
        
        signals.put("D/dtmf0", "org.mobicents.media.server.ctrl.mgcp.pkg.dtmf.DtmfSignal");
        
        //Advanced audio
        signals.put("AU/pa", "org.mobicents.media.server.ctrl.mgcp.pkg.au.Play");
        signals.put("AU/pc", "org.mobicents.media.server.ctrl.mgcp.pkg.au.PlayCollect");
    }
    /**
     * Adds definition of the event;
     * 
     * @param eventName the full name of event including package name
     * @param className the fully qualified name of the class
     */
    public void addEvent(String eventName, String className) {
        if (eventName.indexOf("/") < 0) {
            throw new IllegalArgumentException("The name " + eventName + " is not fully qualified");
        }
        events.put(eventName, className);
    }
    
    /**
     * Adds definition of the signal;
     * 
     * @param eventName the full name of signal including package name
     * @param className the fully qualified name of the class
     */
    public void addSignal(String eventName, String className) {
        if (eventName.indexOf("/") < 0) {
            throw new IllegalArgumentException("The name " + eventName + " is not fully qualified");
        }
        signals.put(eventName, className);
    }
    
    /**
     * Loads specified signal.
     * 
     * @param eventName the fully qualified name of signal including package name.     * 
     * @return signal executor object.
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */     
    public Signal loadSignal(String eventName) throws ClassNotFoundException, InstantiationException, 
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("Loading signal/event handler " + eventName);
        
        if (!signals.containsKey(eventName)) {
            throw new IllegalArgumentException("Event/signal " + eventName + " is not implemented");
        }
        
        String className = signals.get(eventName);
        
        ClassLoader loader = this.getClass().getClassLoader();        
        Class def =loader.loadClass(className);
        
        //select constructor with string argument;
        Constructor constructor = null;
        Constructor[] constructors = def.getConstructors();
        
        for (Constructor cons : constructors) {
            Class[] paramTypes = cons.getParameterTypes();
            if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                constructor = cons;
                break;
            }
        }
        
        if (constructor == null) {
            throw new InstantiationException("Signal must have constructor with string name");
        }
        
        Signal s = (Signal)constructor.newInstance(eventName);
        logger.info("Signal/event handler " + eventName + " successfully loaded");
        
        return s;
    }
}
