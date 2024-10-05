package com.avatarai;

import java.util.Arrays;

public class Feature {
	private double[] values;

	Feature(int size, double value) {
		values = new double[size];
		for (int i = 0; i < size; i++) {
			values[i] = value;
		}
	}

	Feature(double[] newValues) {
		values = newValues.clone();
	}

	public Feature truncate(int size) {
		double[] newValues = new double[size];
        System.arraycopy(values, 0, newValues, 0, size);
		return new Feature(newValues);
	}

	public void normalise()
	{
		double magnitude = getMagnitude();
		scale(1.0/magnitude);
	}

	public void scale(double factor)
	{
		for (int i = 0; i < values.length; i++) {
			values[i] = values[i] * factor;
		}
	}

	// Compare this vector with another for equality of direction and magnitude
	// Returns a value between -1 (diametrically opposed) and +1 (exactly the same)
	public double compare(Feature v) {
		int shortest = Math.min(v.values.length, this.values.length);
		double match = 0.0;

		// Calculate the dot product of the vectors
		for (int i = 0; i < shortest; i++) {
			match += v.values[i] * this.values[i];
		}

		// Normalise the result to give the cosine of the angle between the vectors
		// (A.B = ab cos(theta) where a and b are the magnitudes of vectors A and B)
		if (match != 0.0)
			match = match / (v.getMagnitude() * this.getMagnitude());

		// Now scale the answer by applying a gaussian distribution to the difference in magnitudes
		// (Scales by 1 for matching vector lengths and decreasing scale for mismatching lengths)
//		match *= getGaussianValue(this.getMagnitude()-v.getMagnitude(), 0, 0.5); // Decays to almost zero for an X value of 1 or -1)

		return match; // We should now have a value between 1 (perfect match) and -1 (perfect mismatch)
	}

	// Get the Y value for a given X value on the gaussian distribution curve
	public static double getGaussianValue(double x, double mean, double stdev) {
		return Math.pow(Math.exp(-(((x - mean) * (x - mean)) / ((2 * stdev * stdev)))), 1 / (stdev * Math.sqrt(2 * Math.PI)));
	}

	// Blend another vector with this one by calculating the weighted average of the two
	// A weight of zero leaves this vector unchanged. 
	// A weight of 1 replaces it with the new vector.
	// All values in the vector are limited to +/-1
	public Feature blend(Feature v, double weight) {
		double direction = 1.0;
		if (weight < 0.0) {
			weight = -weight;
			direction = -direction;
		}
		if (v.values.length > this.values.length) {
			values = Arrays.copyOf(values, v.values.length);
		}
		for (int i = 0; i < values.length; i++) {
			values[i] = Math.max(Math.min(values[i] * (1.0 - weight) + v.values[i] * direction * weight, 1.0), -1.0);
		}
		return this;
	}

	public int getLength() {
		return values.length;
	}

	public double getMagnitude() {
		double mag = 0;
        for (double value : values) {
            mag += value * value;
        }
		return Math.sqrt(mag);
	}

	public double[] getValues() {
		return values;
	}

	public void setValues(double[] values) {
		this.values = values;
	}
}

