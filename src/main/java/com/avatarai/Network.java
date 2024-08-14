package com.avatarai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Vector;

public class Network
{
	protected final Vector<Neuron> neurons;
	protected final Vector<Neuron> nerves;
	protected final int numOuts;
	protected final int numLayers;
	protected final int layerWidth;
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
		int width = json.get("width").getAsInt();
		int layers = json.get("layers").getAsInt() - 2;
		JsonArray neuronSettings = json.getAsJsonArray("neurons");

		// Create the vanilla network
		Network net = new Network(name, description, inputs, outputs, width, layers);
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

	public Network(String name, String description, int inputs, int outputs, int width, int hiddenLayers)
	{
		int size = width * (hiddenLayers + 1) + outputs;
		netName = name;
		netDescription = description;
		numOuts = outputs;
		numLayers = 2 + hiddenLayers;
		layerWidth = width;

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

		// Connect each input to neurons in first layer
		for (int i=0; i<inputs; i++)
		{
			Neuron nerve = nerves.elementAt(i+1); // Skip bias input
			for (int j=0; j<width; j++)
			{
				Neuron neuron = neurons.elementAt(j);
				neuron.connect(nerve);
			}
		}

		// Fully forward connect the network
		// For each layer (apart from output layer)
		for (int layer=0; layer<=hiddenLayers; layer++)
		{
			// For each neuron in this layer
			for (int from=0; from<width; from++)
			{
				Neuron fromNeuron = neurons.elementAt(from+(layer*width));
				// For each neuron in the next layer (Note: output layer is different size to hidden layers)
				int nextLayerSize = (layer == hiddenLayers) ? outputs: width;
				for (int to=0; to<nextLayerSize; to++)
				{
					Neuron toNeuron = neurons.elementAt(to+((layer+1)*width));
					toNeuron.connect(fromNeuron);
				}
			}
		}
	}

	public int getOutputCount() {
		return numOuts;
	}

	public int getLayerCount() {
		return numLayers;
	}

	public int getInputCount() {
		return nerves.size()-1;
	}

	public int getLayerWidth() {
		return layerWidth;
	}

	public double getOutput(int number) {
		return getOutputNeuron(number).getOutput();
	}

	private Neuron getOutputNeuron(int number) {
		return neurons.elementAt(neurons.size()-number-1);
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

	public double getInput(int number)
	{
		Neuron nerve = nerves.elementAt(number+1); // Add one to skip bias input
		return nerve.getExcitation();
	}

	// Allows the setting of an error signal on an output neuron to be backpropagated
	public void setError(int number, double value)
	{
		getOutputNeuron(number).setError(value);
	}

	/*
	 * This teach method applies backpropoagation based on output errors set by the external caller
	 */
	public void teach(double rate)
	{
		for (int i=neurons.size()-1; i>=0; i--)
		{
			Neuron neuron = neurons.elementAt(i);
			neuron.backPropagate();
		}

		for (int i=0; i<neurons.size(); i++)
		{
			Neuron neuron = getOutputNeuron(i);
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
		json.addProperty("width", this.getLayerWidth());
		json.addProperty("layers", this.getLayerCount());
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
