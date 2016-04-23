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

package org.mobicents.media.server.mgcp.endpoint.naming;

/**
 *
 * @author yulian oifa
 */
public class EndpointName {
    private String category;
    private String id;
    
    public EndpointName(String name) {
        String tokens[] = name.split("/");
        this.id = tokens[tokens.length - 1];
        
        this.category = "";
        for (int i = 0; i < tokens.length - 1; i++) {
            category += tokens[i];
            if (i < tokens.length - 2) {
                category += "/";
            }
        }
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getID() {
        return id;
    }
}
