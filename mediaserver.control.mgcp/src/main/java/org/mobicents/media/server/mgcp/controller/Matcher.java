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
package org.mobicents.media.server.mgcp.controller;

/**
 *
 * @author kulikov
 */
public class Matcher {
    public boolean match(String name1, String name2) {
        String[] tokens1 = name1.split("/");
        String[] tokens2 = name2.split("/");
        
        if (tokens1.length != tokens2.length) {
            return false;
        }
        
        for (int i = 0; i < tokens1.length; i++) {
            if (tokens1[i].equals("$") || tokens2[i].equals("$")) {
                continue;
            }
            
            if (!tokens1[i].equalsIgnoreCase(tokens2[i])) {
                return false;
            }
        }
        
        return true;
    }
}
