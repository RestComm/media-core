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
package org.mobicents.media.server.mgcp.pkg;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Package factory reads external XML descriptor which provides information 
 * required for construction signal executors and event detectors.
 * 
 * @author kulikov
 */
public class PackageFactory {
	
    private HashMap<String, PackageDescriptor> packages = new HashMap<String, PackageDescriptor>();
    
    /**
     * Loads MGCP package definitions and constructs signal executors.
     * 
     * @param node the <package> node of the configuration.
     */
    public PackageFactory(org.w3c.dom.Node node) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeName() != null && list.item(i).getNodeName().equalsIgnoreCase("package")) {
                loadPackage(list.item(i));
            }
        }
    }
    
    /**
     * Loads definition of the package.
     * 
     * @param descriptor 
     */
    private void loadPackage(Node descriptor) {
        //get the name of the fsm
        NamedNodeMap attributes = descriptor.getAttributes();
        String name =  attributes.getNamedItem("name").getNodeValue();
        
        //create package descriptor instance
        PackageDescriptor pkg = new PackageDescriptor(name);
        
        //load executors
        NodeList list = descriptor.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeName().equals("signal")) {
                createExecutor(pkg, list.item(i));
            }
        }
        
        packages.put(name, pkg);
    }
    
    /**
     * Loads definition of signal executor.
     * 
     * @param pkg the package 
     * @param descriptor executor descriptor xml node
     */
    private void createExecutor(PackageDescriptor pkg, Node descriptor) {
        NamedNodeMap attributes = descriptor.getAttributes();
        
        String name = attributes.getNamedItem("name").getNodeValue();
        String className = attributes.getNamedItem("handler").getNodeValue();
        
        ExecutorDescriptor executor = new ExecutorDescriptor(name, className);
        pkg.add(executor);
    }
    
    /**
     * Constructs the new package instance.
     * 
     * @param name the name of the package to be returned.
     * @return the package. 
     */
    public MgcpPackage getPackage(String name) throws Exception {
        PackageDescriptor descriptor = packages.get(name);
        return descriptor != null ? descriptor.getPackage() : null;
    }
    
    private class PackageDescriptor {
        //package name
        private String name;
        //descriptors of the signal/event executors
        private ArrayList<ExecutorDescriptor> descriptors = new ArrayList<ExecutorDescriptor>();
        
        public PackageDescriptor(String name) {
            this.name = name;
        }
        
        public void add(ExecutorDescriptor descriptor) {
            descriptors.add(descriptor);
        }
        
        public MgcpPackage getPackage() throws Exception {
            ArrayList<Signal> signals = new ArrayList<Signal>();
            for (ExecutorDescriptor descriptor : descriptors) {
                signals.add(descriptor.load());
            }
            return new MgcpPackage(name, signals);
        }
    }
    
    private class ExecutorDescriptor {
        // the name of the signal/event
        private String name;
        //class name implementing this executor
        private String className;
        
        public ExecutorDescriptor(String name, String className) {
            this.name = name;
            this.className = className;
        }
        
        public Signal load() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            ClassLoader loader = this.getClass().getClassLoader();
            Class def = loader.loadClass(className);

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

            Signal s = (Signal) constructor.newInstance(name);
            return s;
        }
    }
}
