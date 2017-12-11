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

import java.util.ArrayList;
import java.util.Collections;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;

/**
 *
 * @author kulikov
 */
public class Patterns {
    private ArrayList<Pattern> patterns = new ArrayList();
    
    public Patterns(Parameter[] labels, Parameters options) {
        if (labels == null || labels.length == 0) {
            return;
        }
        
        if (options == null || options.size() == 0) {
            return;
        }
        
        for (int i = 0; i < labels.length; i++) {
            patterns.add(new Pattern(i, (String) options.get(labels[i])));
        }
        Collections.sort(patterns);
    }    
    
    public boolean hasMore() {
        return patterns.size() > 0;
    }
    
    public Pattern next() {
        return patterns.remove(0);
    }
}
