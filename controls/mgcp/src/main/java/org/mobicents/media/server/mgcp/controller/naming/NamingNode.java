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
import org.mobicents.media.server.utils.Text;

/**
 * The node in the naming tree.
 * 
 * @author kulikov
 */
public class NamingNode<T> {
    //The name of the node
    private Text name;
    
    //parent node
    private NamingNode parent;
    
    //child nodes
    private ArrayList<NamingNode> childs = new ArrayList();
    
    //any attachment to this node
    private T attchment;
    
    /**
     * Create new instance of Naming node
     * 
     * @param name the name of this node.
     * @param parent the parent node.
     */
    public NamingNode(Text name, NamingNode parent) {
        this.name = name;
        this.parent = parent;
    }
    
    /**
     * Gets the name of this node.
     * 
     * @return the name of the node.
     */
    public Text getName() {
        return name;
    }
    
    /**
     * Gets the parent node.
     * 
     * @return parent node.
     */
    public NamingNode getParent() {
        return parent;
    }
    
    /**
     * Creates new child node.
     * 
     * @param name the name of child node to be created
     * @return created node.
     */
    public NamingNode<T> createChild(Text name) {
        NamingNode<T> node = new NamingNode(name, this);
        childs.add(node);
        return node;
    }

    /**
     * Gets the node with specified path
     * 
     * @param path the path to node
     * @param start the position of first path token
     * @param n the length of the path
     * @return the node if exist or null;
     */
    private NamingNode find(Text[] path, int start, int n) {
        //end of tree reached
        if (start == n) {
            return this;
        }
        
        for (NamingNode child : childs) {
            if (child.name.equals(path[start])) {
                return child.find(path, start + 1, n);
            }
        }
        
        //not found
        return null;
    }
    
    /**
     * Gets the node specified by the path
     * 
     * @param path the path to the requested node
     * @param n the length of the path
     * @return the node if exist or null.
     */
    public NamingNode find(Text[] path, int n) {
        return find(path, 0, n);
    }
    
    /**
     * Attaches object to this node.
     * 
     * @param attachment the object to be attached
     */
    public void attach(T attachment) {
        this.attchment = attachment;
    }
    
    /**
     * Gets the object attached to this node.
     * 
     * @return object attached to this node or null if nothing is attached.
     */
    public T poll() {
        return attchment;
    }
    
    @Override
    public String toString() {
        return name.toString();
    }
}

