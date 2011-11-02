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
package org.mobicents.javax.media.mscontrol.mediagroup.signals;

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
