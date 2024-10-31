/*
 * Created on 13 Feb 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.avatarai;

import com.avatarai.maths.Complex;
import com.avatarai.maths.SignalProcessor;

public class SigProcTest
{

	public static void main(String[] args)
	{
		double[] data = new double[1024];
		Complex[] cData = new Complex[1024];
		double f = 250.0; // freq of signal (250Hz)
		double fs = 1024; // sampling freq (5kHz)
		for (int i=0; i<data.length; i++)
		{
			data[i] = Math.sin(2.0*Math.PI*i*f/fs);
			cData[i] = new Complex(data[i], 0.0);
		}
		System.out.println("-----------------------------------------");
		System.out.println("-DISCRETE FFT----------------------------");
		double time1 = System.currentTimeMillis();
		Complex[] results;
		results = SignalProcessor.dft(data);
		double time2 = System.currentTimeMillis();
		System.out.println("Time taken (ms): " + (time2-time1));
		for (Complex result : results) {
			System.out.println(result.abs());
		}

		System.out.println("-----------------------------------------");
		System.out.println("-RAW FFT---------------------------------");
		time1 = System.currentTimeMillis();
		results = SignalProcessor.rawfft(cData);
		time2 = System.currentTimeMillis();
		System.out.println("Time taken (ms): " + (time2-time1));
		for (Complex result : results) {
			System.out.println(result.abs());
		}

		System.out.println("-----------------------------------------");
		System.out.println("-SCALED FFT------------------------------");
		time1 = System.currentTimeMillis();
		results = SignalProcessor.fft(data);
		time2 = System.currentTimeMillis();
		System.out.println("Time taken (ms): " + (time2-time1));
		for (Complex result : results) {
			System.out.println(result.abs());
		}

		System.out.println("-----------------------------------------");
		System.out.println("-INVERSE FFT-----------------------------");
		time1 = System.currentTimeMillis();
		data = SignalProcessor.ifft(results);
		time2 = System.currentTimeMillis();
		System.out.println(time2-time1);
		for (double datum : data) {
			System.out.println(datum);
		}

	}

}
