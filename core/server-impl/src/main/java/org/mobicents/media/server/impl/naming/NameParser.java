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

package org.mobicents.media.server.impl.naming;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author kulikov
 */
public class NameParser {
    public Collection<NameToken> parse(String name) {
        ArrayList<NameToken> list = new ArrayList();
        String[] parts = name.split("/");        
        for (String part: parts) {
            part = part.trim();
            
            if (part.length() == 0) {
                continue;
            }
            
            if (part.startsWith("[")) {
                list.add(new NumericRange(part));
            } else {
                list.add(new FixedToken(part));
            }
            //FIXME: include text ranges
        }
        return list;
    }
}
