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

public class DecAcelp {

	/*-----------------------------------------------------------*
	 *  Function  decod_ACELP()                                  *
	 *  ~~~~~~~~~~~~~~~~~~~~~~~                                  *
	 *   Algebraic codebook decoder.                             *
	 *----------------------------------------------------------*/

	public static void decod_ACELP(
	 int sign,              /* input : signs of 4 pulses     */
	 int index,             /* input : positions of 4 pulses */
	 float cod[]            /* output: innovative codevector */
	)
	{
	   int pos[] = new int[4];
	   int i, j;

	   /* decode the positions of 4 pulses */

	   i = index & 7;
	   pos[0] = i*5;

	   index >>= 3;
	   i = index & 7;
	   pos[1] = i*5 + 1;

	   index >>= 3;
	   i = index & 7;
	   pos[2] = i*5 + 2;

	   index >>= 3;
	   j = index & 1;
	   index >>= 1;
	   i = index & 7;
	   pos[3] = i*5 + 3 + j;

	   /* find the algebraic codeword */

	   for (i = 0; i < LD8KConstants.L_SUBFR; i++) cod[i] = 0;

	   /* decode the signs of 4 pulses */

	   for (j=0; j<4; j++)
	   {

	     i = sign & 1;
	     sign >>= 1;

	     if (i != 0) {
	       cod[pos[j]] = (float)1.0;
	     }
	     else {
	       cod[pos[j]] = (float)-1.0;
	     }
	   }

	   return;
	}

}
