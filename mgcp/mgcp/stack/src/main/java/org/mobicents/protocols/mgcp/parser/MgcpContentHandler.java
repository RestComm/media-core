/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/*
 * File Name     : MgcpContentHandler.java
 *
 * The JAIN MGCP API implementaion.
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */

package org.mobicents.protocols.mgcp.parser;

import java.text.ParseException;

/**
 * Receive notification of the logical content of a message. 
 *
 * @author Oleg Kulikov
 * @author Pavel Mitrenko
 */
public interface MgcpContentHandler {
    /** 
     * Receive notification of the header of a message.
     * Parser will call this method to report about header reading.
     *
     * @param header the header from the message.
     */
    public void header(String header) throws ParseException;
    
    /**
     * Receive notification of the parameter of a message.
     * Parser will call this method to report about parameter reading.
     *
     * @param name the name of the paremeter
     * @param value the value of the parameter.
     */
    public void param(String name, String value) throws ParseException;
    
    /**
     * Receive notification of the session description.
     * Parser will call this method to report about session descriptor reading.
     *
     * @param sd the session description from message.
     */
    public void sessionDescription(String sd) throws ParseException;
    
}
