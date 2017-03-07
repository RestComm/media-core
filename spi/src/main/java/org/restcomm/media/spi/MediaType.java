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

package org.restcomm.media.spi;

/**
 *
 * @author kulikov
 */
public enum MediaType {
    AUDIO(0, "audio", 0x01), VIDEO(1, "video", 0x02);
    
    private int code;
    private String name;
    private int mask;

    MediaType(int code, String name, int mask) {
        this.code = code;
        this.name = name;
        this.mask = mask;
    }
    
    public static MediaType getInstance(String name) {
        if (name.equalsIgnoreCase("audio")) {
            return AUDIO;
        } else if(name.equalsIgnoreCase("video")){
            return VIDEO;
        }else
        {
        	throw new IllegalArgumentException("There is no media type for: "+name);
        }
    }

    public static MediaType getMediaType(int code) {
        if (code == 0) {
            return AUDIO;
        } else {
            return VIDEO;
        }
    }
    
    public int getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public int getMask(){
    	return mask;
    }
    
}
