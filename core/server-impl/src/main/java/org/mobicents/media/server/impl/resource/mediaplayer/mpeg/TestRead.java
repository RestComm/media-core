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

package org.mobicents.media.server.impl.resource.mediaplayer.mpeg;

import java.io.RandomAccessFile;

/**
 *
 * @author kulikov
 */
public class TestRead {
    public static void main(String[] args) throws Exception {
        RandomAccessFile file = new RandomAccessFile("c:\\video\\001.mp4", "r");
        file.seek(23360 -4);
        
        byte[] buffer = new byte[1000];
        file.read(buffer);
        
        for (int i = 0; i < 20; i++) {
            System.out.println(Integer.toHexString(buffer[i]));
        }
    }
}
