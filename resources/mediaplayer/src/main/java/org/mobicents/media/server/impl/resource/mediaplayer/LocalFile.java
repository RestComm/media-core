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

package org.mobicents.media.server.impl.resource.mediaplayer;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents media file url in a local file system
 * 
 * @author kulikov
 */
public class LocalFile {
    private String fileName;
    private String folder;
    
    public LocalFile(String home) throws IllegalArgumentException {
        if (home == null) {
            home = System.getenv("MMS_HOME");        
            if (home == null) {
                throw new IllegalArgumentException("MMS_HOME not set");
            }
        }
        
        folder = home + "/media/";
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public URL getURL() {
        try {
            return new URL(folder + fileName);
        } catch (MalformedURLException e) {
            //should never happen
            return null;
        }
    }
}
