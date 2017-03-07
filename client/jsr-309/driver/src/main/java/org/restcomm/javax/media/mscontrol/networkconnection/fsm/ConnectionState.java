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

package org.restcomm.javax.media.mscontrol.networkconnection.fsm;

/**
 *
 * @author kulikov
 */
public interface ConnectionState {    
    public final static String NULL = "NULL";
    public final static String OPENING = "OPENING";
    public final static String OPEN= "OPEN";
    public final static String HALF_OPEN = "HALF_OPEN";
    public final static String CANCELED = "CANCELED";
    public final static String MODIFYING = "MODIFYING";
    public final static String CLOSING = "CLOSING";
    public final static String FAILED = "FAILED";
    public final static String INVALID = "INVALID";    
}
