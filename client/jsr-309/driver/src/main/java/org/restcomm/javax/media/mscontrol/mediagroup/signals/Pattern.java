/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol.mediagroup.signals;

/**
 *
 * @author kulikov
 */
public class Pattern implements Comparable {
    private int index;
    private String value;
    
    public Pattern(int index, String value) {
        this.index = index;
        this.value = value;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getValue() {
        return value;
    }
    
    public boolean matches(String text) {
        return value.matches(text);
    }

    public int compareTo(Object o) {
        Pattern other = (Pattern)o;
        
        if (this.value.length() > other.value.length()) {
            return 1;
        } 
        
        if (this.value.length() == other.value.length()) {
            return 0;
        } 
        
        return -1;
    }
}
