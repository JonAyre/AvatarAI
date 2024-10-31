package com.avatarai.neural;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Vector;

public class Network
{
	protected final Vector<Neuron> neurons;
	protected final Vector<Neuron> nerves;
	protected final int numOuts;
	protected final int[] layerSizes;
	private final String netName;
	private final String netDescription;

	public static Network fromString(String networkJson)
	{
		JsonObject json = new Gson().fromJson(networkJson, JsonObject.class);
		// Get the network properties
		String name = json.get("name").getAsString();
		String description = json.get("description").getAsString();

		int inputs = json.get("inputs").getAsInt();
		int outputs = json.get("outputs").getAsInt();
		JsonArray layersJson = json.get("layers").getAsJsonArray();
		int[] layers = new int[layersJson.size()];
		for (int i = 0; i < layersJson.size(); i++)
		{
			layers[i] = layersJson.get(i).getAsInt();
		}
		JsonArray neuronSettings = json.getAsJsonArray("neurons");

		// Create the vanilla network
		Network net = new Network(name, description, inputs, outputs, layers);
		for (int i=0; i<net.neurons.size(); i++)
		{
			Neuron neuron = net.neurons.get(i);
			JsonArray weights = neuronSettings.get(i).getAsJsonArray();
			for (int j=0; j<neuron.inputs.size(); j++)
			{
				neuron.inputs.get(j).setWeight(weights.get(j).getAsDouble());
			}
		}
		return net;
	}

	public Network(String name, String description, int inputs, int outputs, int[] layers)
	{
		// Total number of neurons is number of output neurons plus number in each layer
		int size = outputs;
		layerSizes = new int[layers.length];
		for (int i=0; i<layers.length; i++)
		{
			size += layers[i];
			layerSizes[i] = layers[i];
		}
		netName = name;
		netDescription = description;
		numOuts = outputs;

		// Create a single input to act as a fixed bias input to all neurons in the network
		Neuron biasInput = new Neuron(-1, 1.0);
		biasInput.setExcitation(1.0); // Fix the bias input level at 1.0
		biasInput.propagate(); // Push fixed input to output value

		// Create the input nerves and add the bias input as the first one
		nerves = new Vector<>();
		nerves.add(biasInput);
		for (int i=0; i<inputs; i++)
		{
			nerves.add(new Neuron(-1, 1.0));
		}

		// Create all the neurons for the network ready for inter-connection and connect the bias input
		neurons = new Vector<>();
		for (int i=0; i<size; i++)
		{
			Neuron cell = new Neuron(i,1.0); // Create a new neuron
			cell.connect(biasInput); // Connect it to a bias input
			neurons.add(cell); // Add it to the network
		}

		// Connect each input to neurons in first layer (layer 0)
		for (int i=0; i<inputs; i++)
		{
			Neuron nerve = nerves.elementAt(i+1); // Skip bias input
			for (int j=0; j<layers[0]; j++)
			{
				Neuron neuron = neurons.elementAt(j);
				neuron.connect(nerve);
			}
		}

		int offset = 0; // USed to keep track of how far through the network we are when creating connections

		// Fully forward connect the network
		// For each layer (apart from output layer)
		for (int layer=0; layer<layers.length; layer++)
		{
			// For each neuron in this layer
			for (int from=0; from<layers[layer]; from++)
			{
				Neuron fromNeuron = neurons.elementAt(from+offset);
				// For each neuron in the next layer (Note: output layer size is defined by number of outputs)
				int nextLayerSize = (layer == layers.length-1) ? outputs: layers[layer+1];
				for (int to=0; to<nextLayerSize; to++)
				{
					Neuron toNeuron = neurons.elementAt(to+offset+layers[layer]);
					toNeuron.connect(fromNeuron);
				}
			}

			offset += layers[layer]; // Increase offset to reference neurons in the next layer
		}
	}

	public int getOutputCount() {
		return numOuts;
	}

	public int getInputCount() {
		return nerves.size()-1;
	}

//	public double getOutput(int number) {
//		return getOutputNeuron(number).getOutput();
//	}

	public double[] getOutputs() {
		return getLayerOutputs(-1);
	}

	public int[] getLayerSizes() {return layerSizes;}

	public double[] getLayerOutputs(int layer) {
		int offset = 0;
		int numToGet = 0;
		if (layer == -1) {
			offset = neurons.size() - numOuts;
			numToGet = numOuts;
		} else {
			for (int i=0; i<layer; i++) {
				offset += layerSizes[i];
			}
			numToGet = layerSizes[layer];
		}
		double[] outputs = new double[numToGet];
		for (int i=0; i<numToGet; i++) {
			outputs[i] = neurons.elementAt(offset+i).getOutput();
		}
		return outputs;
	}

	public void propagate()
	{
		for (int i=1; i<nerves.size(); i++) // Skip bias input - already propagated
		{
			Neuron nerve = nerves.elementAt(i);
			nerve.propagate();
		}

		for (int i=0; i<neurons.size(); i++)
		{
			Neuron neuron = neurons.elementAt(i);
			neuron.propagate();
		}
	}

	public void setInput(int number, double value)
	{
		Neuron nerve = nerves.elementAt(number+1); // Add one to skip bias input
		nerve.setExcitation(value);
	}

	// Allows the setting of an error signal on an output neuron to be backpropagated
	public void setError(int number, double value)
	{
		neurons.elementAt(neurons.size()-numOuts+number).setError(value);
	}

	/*
	 * Apply backpropagation based on output errors set by the external caller
	 */
	public void teach(double rate)
	{
		for (int i=neurons.size()-1; i>=0; i--)
		{
			Neuron neuron = neurons.elementAt(i);
			neuron.backPropagate();
		}

		for (int i=neurons.size()-1; i>=0; i--)
		{
			Neuron neuron = neurons.elementAt(i);
			neuron.teach(rate);
		}
	}

	// Serialise the network in a manner suitable for saving for later use
	public String toString()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", this.netName);
		json.addProperty("description", this.netDescription);
		json.addProperty("inputs", this.getInputCount());
		json.addProperty("outputs", this.getOutputCount());

		JsonArray layerList = new JsonArray();
        for (int layerSize : layerSizes) {
            layerList.add(layerSize);
        }
		json.add("layers", layerList);

		JsonArray neuronList = new JsonArray();
		for (Neuron neuron: neurons)
		{
			JsonArray weightList = new JsonArray();
			for (Synapse synapse: neuron.inputs)
			{
				weightList.add(Math.round(10000 * synapse.weight) / 10000.0);
			}
			neuronList.add(weightList);
		}

		json.add("neurons", neuronList);

		return json.toString();
	}
}
