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

package org.restcomm.media.codec.g729;

public class PParity {

	/*-----------------------------------------------------*
	 * parity_pitch - compute parity bit for first 6 MSBs  *
	 *-----------------------------------------------------*/

	public static int parity_pitch(     /* output: parity bit (XOR of 6 MSB bits)     */
	  int pitch_index     /* input : index for which parity is computed */
	)
	{
	    int temp, sum, i, bit;

	    temp = pitch_index >> 1;

	    sum = 1;
	    for (i = 0; i <= 5; i++) {
	        temp >>= 1;
	        bit = temp & 1;
	        sum = sum + bit;
	    }
	    sum = sum & 1;
	    return (sum);
	}

	/*--------------------------------------------------------------------*
	 * check_parity_pitch - check parity of index with transmitted parity *
	 *--------------------------------------------------------------------*/

	public static int check_parity_pitch(  /* output: 0 = no error, 1= error */
	  int pitch_index,       /* input : index of parameter     */
	  int parity             /* input : parity bit             */
	)
	{
	    int temp, sum, i, bit;
	    temp = pitch_index >> 1;

	    sum = 1;
	    for (i = 0; i <= 5; i++) {
	        temp >>= 1;
	        bit = temp & 1;
	        sum = sum + bit;
	    }
	    sum += parity;
	    sum = sum & 1;
	    return (sum);
	}

}
