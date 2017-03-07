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

package org.restcomm.javax.media.mscontrol;

import javax.media.mscontrol.Parameter;

/**
 *
 * @author kulikov
 */
public class ParameterImpl implements Parameter {
    private String name;
    
    public static Parameter create(String name) {
        return new ParameterImpl(name);
    }
    
    private ParameterImpl(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        
        if (other == this) {
            return true;
        }
        
        if (!(other instanceof ParameterImpl)) {
            return false;
        }
        
        return ((ParameterImpl) other).name.equals(name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
