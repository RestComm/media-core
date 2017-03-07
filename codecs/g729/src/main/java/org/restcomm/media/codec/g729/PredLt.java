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

public class PredLt {


	/*-------------------------------------------------------------------*
	 * Function  Pred_lt_3()                                             *
	 *           ~~~~~~~~~~~                                             *
	 *-------------------------------------------------------------------*
	 * Compute the result of long term prediction with fractional        *
	 * interpolation of resolution 1/3.                                  *
	 *                                                                   *
	 * On return exc[0..L_subfr-1] contains the interpolated signal      *
	 *   (adaptive codebook excitation)                                  *
	 *-------------------------------------------------------------------*/

	public static void pred_lt_3(         /* Compute adaptive codebook                       */
	  float exc[],int excs,          /* in/out: excitation vector, exc[0:l_sub-1] = out */
	  int t0,               /* input : pitch lag                               */
	  int frac,             /* input : Fraction of pitch lag (-1, 0, 1)  / 3   */
	  int l_subfr           /* input : length of subframe.                     */
	)
	{

	  int   i, j, k;
	  float s;
	  int x0, x1, x2, c1, c2;


	  x0 = -t0;//&exc[-t0];

	  frac = -frac;
	  if (frac < 0) {
	    frac += LD8KConstants.UP_SAMP;
	    x0--;
	  }

	  for (j=0; j<l_subfr; j++)
	  {
	    x1 = x0++;
	    x2 = x0;
	    c1 = frac;//&inter_3l[frac];
	    c2 = LD8KConstants.UP_SAMP-frac;//&inter_3l[LD8KConstants.UP_SAMP-frac];

	    s = (float)0.0;
	    for(i=0, k=0; i< LD8KConstants.L_INTER10; i++, k+=LD8KConstants.UP_SAMP)
	      s+= exc[excs+x1-i] * TabLD8k.inter_3l[c1+k] + exc[excs+x2+i] * TabLD8k.inter_3l[c2+k];

	    exc[excs+j] = s;
	  }

	  return;
	}

}
