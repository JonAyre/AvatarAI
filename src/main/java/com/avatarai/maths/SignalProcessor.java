/*
 * Created on 11 Feb 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.avatarai.maths;

public class SignalProcessor
{
	private static final double PI = Math.PI;
	
	// Performs a discrete fourier transform on the supplied data
	// Returns an array of complex numbers containing each of the frequency elements of the result.
	// NOTE: By setting lowf to zero and highf to 1.0 gives the same result as the fft.
	//       Frequency range of result is from 0 to sampling frequency.
	public static Complex[] dft(double[] data)
	{
		return dft(data, 0, 0.0, 1.0);
	}

	// Performs a discrete fourier transform on the supplied data
	// Returns an array of complex numbers containing each of the frequency elements of the result.
	public static Complex[] dft(double[] data, 	// incoming real data
	                            int zero,		// the index of the incoming data sample that represents zero time
	                            double lowF, 	// Low freq limit as fraction of sampling frequency
	                            double highF) 	// High freq limit as fraction of sampling frequency
	{
		int dataLen = data.length;
		Complex[] results = new Complex[dataLen];
		double delF = (highF-lowF)/data.length;
		//Outer loop iterates on frequency values.
		for (int i=0; i<dataLen; i++)
		{
			double freq = lowF + i*delF;
			double real = 0.0;
			double imag = 0.0;
			//Inner loop iterates on time-series points.
			for (int j=0; j<dataLen; j++)
			{
				real += data[j]*Math.cos(2*PI*freq*(j-zero));
				imag += data[j]*Math.sin(2*PI*freq*(j-zero));
			}//end inner loop
			results[i] = new Complex(real/dataLen, imag/dataLen);
		}//end outer loop
		return results;
	}//end transform method


	// Compute the FFT of data[], assuming its length is a power of 2
	// The input data is scaled by data.length in order to ensure that the result is
	// of the correct magnitude. (Uses rawfft to do the actual work). 
	// Frequency range of result is always from 0 to sampling frequency
	// but only half the range is usable (i.e. to half the sampling frequency
	// because it mirrors itself beyond that point)
	public static Complex[] fft(double[] data)
	{
		Complex[] cdata = new Complex[data.length];
		for (int i=0; i<data.length; i++)
		{
			cdata[i] = new Complex(data[i]/data.length, 0.0);
		}
		return rawfft(cdata);
	}

    // Compute the raw FFT of x[], assuming its length is a power of 2 and its contents are Complex
	// The magnitude of the result is larger by a factor of the length of the data 
	// (i.e. needs dividing by x.length)
	// Frequency range of result is always from 0 to sampling frequency.
    public static Complex[] rawfft(Complex[] x) 
	{
		int N = x.length;
		
		// base case
		if (N == 1) return new Complex[] {x[0]};
		
		// radix 2 Cooley-Tukey FFT
		if (N % 2 != 0) throw new RuntimeException("N is not a power of 2");
		
		// fft of even terms
		Complex[] term = new Complex[N/2];
		for (int k = 0; k < N/2; k++) term[k] = x[2*k];
		Complex[] q = rawfft(term);
		
		// fft of odd terms
		for (int k = 0; k < N/2; k++) term[k] = x[2*k + 1];
		Complex[] r = rawfft(term);
		
		// combine
		Complex[] y = new Complex[N];
		for (int k = 0; k < N/2; k++) 
		{
			double kth = -2 * k * Math.PI / N;
			Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
			y[k]       = q[k].plus(wk.times(r[k]));
			y[k + N/2] = q[k].minus(wk.times(r[k]));
		}
		return y;
	}

	// compute the inverse FFT of x[], assuming its length is a power of 2
	public static double[] ifft(Complex[] x)
	{
		int N = x.length;
		Complex[] y = new Complex[N];
		double[] z = new double[N];

		// take conjugate
		for (int i = 0; i < N; i++)
			y[i] = x[i].conjugate();

		// compute forward FFT
		y = rawfft(y);

		// take conjugate again
		for (int i = 0; i < N; i++)
			y[i] = y[i].conjugate();

		// Copy real part of result to double array result (signal is real)
		for (int i = 0; i < N; i++)
			z[i] = y[i].Re();

		return z;
	}
}
