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

package org.restcomm.jsr309.mgcp;

import org.restcomm.media.client.mgcp.jain.pkg.AUMgcpEvent;
import org.restcomm.media.client.mgcp.jain.pkg.AUPackage;

import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

/**
 * Defines advanced audio package.
 * 
 * @author kulikov
 */
public interface PackageAU {
    /** package name */
    public final static PackageName Name = AUPackage.AU;
    
    /** events */
    public final static MgcpEvent pa = AUMgcpEvent.aupa;
    public final static MgcpEvent pc = AUMgcpEvent.aupc;
    public final static MgcpEvent pr = AUMgcpEvent.aupr;
    
    public final static MgcpEvent oc = AUMgcpEvent.auoc;
    public final static MgcpEvent of = AUMgcpEvent.auof;
    public final static MgcpEvent es = AUMgcpEvent.aues;
}
