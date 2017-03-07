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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.media.spi.utils;

/**
 *
 * @author kulikov
 */
public class TimeChecker {
    public static boolean check(long[] set, double tolerance) {
        long min = 0;
        long max = 0;
        long avg = 0;

        for (int i = 0; i < set.length; i++) {
            if (set[i] < min) min = set[i];
            if (set[i] > max) max = set[i];
            avg += set[i];
        }

        avg = avg / set.length;
        double ratio = ((double)(max - min))/max;
        System.out.println(String.format("max=%s, min=%s", max, min));
        return ratio <= tolerance;
    }
}
