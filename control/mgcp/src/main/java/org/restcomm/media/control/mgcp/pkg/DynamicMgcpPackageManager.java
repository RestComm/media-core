/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.pkg;

import java.util.HashMap;
import java.util.Map;

/**
 * A Package Manager that includes all supports available MGCP packages.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DynamicMgcpPackageManager implements MgcpPackageManager {

    private final Map<String, MgcpPackage> packages;

    public DynamicMgcpPackageManager() {
        this.packages = new HashMap<>(5);
    }

    @Override
    public MgcpPackage getPackage(String name) {
        return this.packages.get(name);
    }

    @Override
    public void registerPackage(MgcpPackage pkg) {
        if (!this.packages.containsKey(pkg.getPackageName())) {
            this.packages.put(pkg.getPackageName(), pkg);
        }
    }

    @Override
    public void unregisterPackage(MgcpPackage pkg) {
        this.packages.remove(pkg.getPackageName());
    }

}
