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

package org.mobicents.media;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.naming.InnerNamingService;
import org.mobicents.media.server.impl.resource.zap.Trunk;

/**
 *
 * @author kulikov
 */
public class TrunkManager {
    private Logger logger = Logger.getLogger(TrunkManager.class);
    
    private InnerNamingService namingService;
    private ArrayList<Trunk> trunks = new ArrayList();
    
    public InnerNamingService getNaming() {
        return namingService;
    }
    
    public void setNaming(InnerNamingService namingService) {
        this.namingService = namingService;
    }
    
    public void addTrunk(Trunk trunk) {
        trunks.add(trunk);
//        trunk.setNamingService(namingService);
        logger.info("Added trunk " + trunk);
    }
    
    public void removeTrunk(Trunk trunk) {
        trunks.remove(trunk);
    }
    
    public void start() {
        logger.info("Started with naming service " + namingService);
    }
    
    public void stop() {
        
    }
}
