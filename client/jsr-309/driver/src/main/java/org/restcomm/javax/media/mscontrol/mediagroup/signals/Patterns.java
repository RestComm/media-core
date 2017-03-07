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
