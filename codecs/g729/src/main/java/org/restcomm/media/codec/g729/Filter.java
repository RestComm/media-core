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

public class Filter {


	/*-----------------------------------------------------------*
	 * convolve - convolve vectors x and h and put result in y   *
	 *-----------------------------------------------------------*/

	public static void convolve(
	 float x[], int xs,             /* input : input vector x[0:l]                     */
	 float h[], int hs,             /* input : impulse response or second input h[0:l] */
	 float y[], int ys,            /* output: x convolved with h , y[0:l]             */
	 int  l                 /* input : dimension of all vectors                */
	)
	{
	   float temp;
	   int    i, n;

	   for (n = 0; n < l; n++)
	     {
	        temp = (float)0.0;
	        for (i = 0; i <= n; i++)
	          temp += x[xs+i]*h[hs+n-i];
	        y[ys+n] = temp;
	     }

	   return;
	}

	/*-----------------------------------------------------------*
	 * syn_filt - filter with synthesis filter 1/A(z)            *
	 *-----------------------------------------------------------*/

	public static void syn_filt(
	 float a[],int as,     /* input : predictor coefficients a[0:m]    */
	 float x[],int xs,     /* input : excitation signal                */
	 float y[],int ys,     /* output: filtered output signal           */
	 int  l,        /* input : vector dimension                 */
	 float mem[],int mems,   /* in/out: filter memory                    */
	 int  update    /* input : 0 = no memory update, 1 = update */
	)
	{
	   int  i,j;

	   /* This is usually done by memory allocation (l+m) */
	   float yy_b[] = new float[LD8KConstants.L_SUBFR+LD8KConstants.M];
	   double s;
	   int yy, py, pa;

	   /* Copy mem[] to yy[] */

	   yy = 0;//yy_b;
	   for (i = 0; i <LD8KConstants.M; i++)  yy_b[yy++] =  mem[mems++];

	   /* Filtering */

	   for (i = 0; i < l; i++)
	     {
	        py=yy;
	        pa=0;//a
	        s = x[xs++];
	        for (j = 0; j <LD8KConstants.M; j++)  s -= (a[as+ ++pa]) * (double)(yy_b[--py]);
	        yy_b[yy++] = (float)s;
	        y[ys++] = (float)s;
	     }

	   /* Update memory if required */

	   if(update !=0 ) for (i = 0; i <LD8KConstants.M; i++)  mem[--mems] =yy_b[--yy];

	   return;
	}

	/*-----------------------------------------------------------*
	 * residu - filter input vector with all-zero filter A(Z)    *
	 *-----------------------------------------------------------*/

	public static void residu(    /* filter A(z)                                       */
	 float []a,int as,      /* input : prediction coefficients a[0:m+1], a[0]=1. */
	 float []x,int xs,      /* input : input signal x[0:l-1], x[-1:m] are needed */
	 float []y,int ys,      /* output: output signal y[0:l-1] NOTE: x[] and y[]
	                            cannot point to same array               */
	 int  l        /* input : dimension of x and y                      */
	)
	{
	  double  s;
	  int  i, j;
	  int ya = 0;

	  for (i = 0; i < l; i++)
	  {
	    s = x[xs+i];
	    for (j = 1; j <= LD8KConstants.M; j++) s += a[as+j]*(double)x[xs+i-j];
	    y[ys+ya++] = (float)s;
	  }
	  return;
	}

}
