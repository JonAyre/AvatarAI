package com.avatarai.neural;

public class Synapse
{
	protected double weight;
	protected final Neuron input;
	protected final Neuron output;
	
	public Synapse(Neuron newInput, Neuron newOutput)
	{
		super();
		weight = (Math.random() - 0.5) / 0.5; // -1.0 <= weight <= 1.0
		input = newInput;
		output = newOutput;
		newInput.connectOut(this);
	}

	private double getInputLevel() {
		if (input == null)
			return 0.0;
		else
			return input.getOutput();
	}

	private double getOutputDelta() {
		if (output == null)
			return 0.0;
		else
			return output.getDelta();
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (09/01/02 10:57:14)
	 * @return double
	 */
	public double getOutput() 
	{
		return getInputLevel() * weight;
	}
	public double getDelta() 
	{
		return getOutputDelta() * weight;
	}

	public double teach(double rate) 
	{
		// Calculate weight change according to back-propagation algorithm (i.e. multiply input neuron firing level by output neuron feedback delta)
		double dw;
		dw = rate * getOutputDelta() * getInputLevel();
		weight += dw;
		return weight;
	}

	public String toString() 
	{
		return String.valueOf(weight);
	}

	public double getWeight()
	{
		return weight;
	}

	public void setWeight(double d)
	{
		weight = d;
	}

}
