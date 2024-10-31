package com.avatarai.neural;

import com.google.gson.JsonObject;

import java.util.Vector;

public class Neuron
{
	protected int id;
	protected double backpropError = 0.0;
	protected double outputError = 0.0;
	protected double delta = 0.0;
	protected double gradient = 0.0;
	protected double excitation = 0.0;
    protected double output = 0.0;
	protected double gain;
	protected final Vector<Synapse> inputs;
	protected final Vector<Synapse> outputs;

	public Neuron(int newId, double newGain)
	{
		super();
		id = newId;
		gain = newGain;
		inputs = new Vector<>();
		outputs = new Vector<>();
	}

	public void connect(Neuron input)
	{
		inputs.add(new Synapse(input, this));
	}
	public int getNumInputs() { return inputs.size(); }
	public void connectOut(Synapse outputSyn)
	{
		outputs.add(outputSyn);
	}

	public void teach(double rate)
	{
		for (int i=0; i<inputs.size(); i++)
		{
			Synapse input = inputs.elementAt(i);
			input.teach(rate);
		}
	}

	public String toString()
	{
		JsonObject json = new JsonObject();
		json.addProperty("weights", inputs.toString());
		return json.toString();
	}

	public void setExcitation(double newLevel)
	{
		excitation = newLevel;
	}

	public double getExcitation()
	{
		return excitation;
	}
	public double getOutput()
	{
		return output;
	}

	public void propagate()
	{
		// Calculate sum of inputs as supplied by input synapses. (only if inputs > 0)
		double net = 0.0;
		if (!inputs.isEmpty())
		{
			for (int i=0; i<inputs.size(); i++)
			{
				Synapse input = inputs.elementAt(i);
				net += input.getOutput();
			}
			setExcitation(net/Math.sqrt(getNumInputs()));
		}

		gradient = sigmoidGrad(excitation, gain);
		output = sigmoid(excitation, gain);
	}

	public void backPropagate()
	{
		// Calculate sum of feedback deltas as supplied by output synapses.
		double net = outputError;
		for (int i=0; i<outputs.size(); i++)
		{
			Synapse output = outputs.elementAt(i);
			net += output.getDelta();
		}
		backpropError = net/Math.sqrt(getNumInputs());
		delta = backpropError * gradient; // Back-propagated value = back-propagated error x gradient at current output level
	}

	public static double sigmoid(double value, double gain)
	{
	//	return 1.0/(1.0+Math.exp(-((value-0.5)*gain*10.0))); // Sigmoid function compressed to take values between 0 and 1
		return 1.0/(1.0+Math.exp(-(value*gain*10.0))); // Sigmoid function compressed to take values between -1 and 1
	}

	public static double sigmoidGrad(double value, double gain)
	{
		double val1 = sigmoid(value-0.005, gain);
		double val2 = sigmoid(value+0.005, gain);
		return (val2-val1)/0.01;
	}

	public double getError() { return outputError; }
	public void setError(double newError)
	{
		this.outputError = newError;
	}
	public double getDelta()
	{
		return delta;
	}
}
