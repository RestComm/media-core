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

package org.restcomm.media.spi;
/**
 *
 * @author amit bhayani
 */
public enum ConnectionState {

    NULL(0, "NULL", -1),
    CREATING(1, "CREATING", 5),
    //5 seconds is not enough for anything and basically means that
    //connections should be established only after 200 ok received on sip.
    //but connection should be created when invite received.
    HALF_OPEN(2, "HALF_OPEN", 300),
    OPENING(3, "OPENING", 5),
    //calls can be longer then 30 minutes
    OPEN(4, "OPEN", 240 * 60),
    CLOSING(5, "CLOSING", 5);

    private int code;
    private String stateName;
    private long timeout;

    private ConnectionState(int code, String stateName, long timeout) {
        this.stateName = stateName;
        this.code = code;
        this.timeout = timeout;
    }
    
    public static ConnectionState getInstance(String name) {
        if (name.equalsIgnoreCase("NULL")) {
            return NULL;
        } else if(name.equalsIgnoreCase("HALF_OPEN")){
            return HALF_OPEN;
        } else if(name.equalsIgnoreCase("OPEN")){
            return OPEN;
        } else if (name.equalsIgnoreCase("CREATING")) {
            return CREATING;
        } else if (name.equalsIgnoreCase("OPENING")) {
            return OPENING;
        } else if (name.equalsIgnoreCase("CLOSING")) {
            return CLOSING;
        } else return null;
    }    
    
    @Override
    public String toString() {
        return stateName;
    }
    
    public int getCode() {
        return code;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
