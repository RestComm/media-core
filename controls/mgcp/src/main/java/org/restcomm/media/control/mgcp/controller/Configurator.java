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
package org.restcomm.media.control.mgcp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.restcomm.media.control.mgcp.MgcpProvider;
import org.restcomm.media.control.mgcp.controller.signal.MgcpPackage;
import org.restcomm.media.control.mgcp.pkg.PackageFactory;
import org.restcomm.media.spi.Endpoint;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Loads configuration from external xml file.
 * 
 * @author kulikov
 */
public class Configurator {

    private Matcher matcher = new Matcher();
    
    private PackageFactory packageFactory;
    private ArrayList<EndpointDescriptor> endpoints = new ArrayList<EndpointDescriptor>();
    
    public Configurator(InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        //load parser
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();

        //parse
        Document document = builder.parse(stream);

        //process <document> tag
        NodeList list = document.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeName() != null && list.item(i).getNodeName().equalsIgnoreCase("mgcp")) {
                loadConfig(list.item(i));
            }
        }
    }

    /**
     * Processes <mgcp> tag.
     * 
     * @param node  the <mgcp> node. 
     */
    private void loadConfig(org.w3c.dom.Node node) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            //detected <packages> tag
            if (list.item(i).getNodeName() != null && list.item(i).getNodeName().equalsIgnoreCase("packages")) {
                packageFactory = new PackageFactory(list.item(i));
            }
            
            //detected <endpoints> tag
            if (list.item(i).getNodeName() != null && list.item(i).getNodeName().equalsIgnoreCase("endpoints")) {
                loadEndpoints(list.item(i));
            }
        }
    }
    
    private void loadEndpoints(org.w3c.dom.Node node) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeName().equalsIgnoreCase("endpoint")) {
                NamedNodeMap attributes = list.item(i).getAttributes();
                
                String pattern = attributes.getNamedItem("local-name").getNodeValue();
                String pkgList = attributes.getNamedItem("packages").getNodeValue();
                
                EndpointDescriptor descriptor = new EndpointDescriptor(pattern, pkgList.split(","));
                endpoints.add(descriptor);
            }
        }
    }
    
    /**
     * Creates relation between MGCP endpoint(activity) and server endpoint.
     * 
     * @param endpoint the server endpoint.
     * @param mgcpProvider the MGCP protocol provider;
     * @param address the address of the domain
     * @param port the port number used for listening
     * @return MGCP endpoint.
     */
    public MgcpEndpoint activate(Endpoint endpoint, MgcpProvider mgcpProvider, String address, int port) throws Exception {
        EndpointDescriptor descriptor = null;
        for (EndpointDescriptor d : endpoints) {
            if (matcher.match(d.namePattern, endpoint.getLocalName())) {
                descriptor = d;
                break;
            }
        }
        
        ArrayList<MgcpPackage> packages = new ArrayList<MgcpPackage>();
        
        if (descriptor != null) {
        	for (int i = 0; i < descriptor.packages.length; i++) {
               packages.add(packageFactory.getPackage(descriptor.packages[i].trim()));
        	}        	
        }
        
        return new MgcpEndpoint(endpoint, mgcpProvider, address, port, packages);
    }
    
    /**
     * MGCP endpoint descriptor.
     */
    private class EndpointDescriptor {
        private String namePattern;
        private String[] packages;
        
        public EndpointDescriptor(String namePattern, String[] packages)  {
            this.namePattern = namePattern;
            this.packages = packages;
        }
    }
}
