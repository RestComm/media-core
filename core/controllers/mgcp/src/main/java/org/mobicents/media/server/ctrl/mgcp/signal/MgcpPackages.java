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
package org.mobicents.media.server.ctrl.mgcp.signal;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Set;

/**
 * Holder for package definition and package loader.
 * 
 * @author kulikov
 */
public class MgcpPackages {
    private static final HashMap<String, String[]> defs = new HashMap();
    private static final Signals signals = new Signals();
    
    static {
        //DTMF package
        defs.put("D", new String[] {"dtmf0"});
        
        //Inner tests
        defs.put("T", new String[]{"test1", "test2", "test3"});
        
        //Advanced audio
        defs.put("AU", new String[]{
            "pa", "pc"
        });
    }
    
    public SignalExecutor load(String packageName) throws ClassNotFoundException, InstantiationException, 
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        SignalExecutor pkg = new SignalExecutor(packageName);
        
        if (!defs.containsKey(packageName)) {
            throw new IllegalArgumentException("Unknown package " + packageName);
        }
        
        String[] names = defs.get(packageName);
        for (String name : names) {
            pkg.include(signals.loadSignal(packageName + "/" + name));
        }
        
        return pkg;
    }
    
    public SignalExecutor[] loadAll() throws Exception {
        SignalExecutor[] list = new SignalExecutor[defs.size()];
        Set<String> names = defs.keySet();
        int i = 0;
        
        for (String name: names) {
            list[i++] = load(name);
        }
        
        return list;
    }
}
