package com.avatarai.neural;

import com.avatarai.maths.Matrix;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MatrixNetwork
{
	protected final ArrayList<Matrix> weights = new ArrayList<>();
	protected final ArrayList<Matrix> outputs = new ArrayList<>();
	protected final Matrix errors;

	protected Matrix inputs;
	protected Matrix processedInputs;

	private final String netName;
	private final String netDescription;
	int[] layerSizes;

	public static MatrixNetwork fromString(String networkJson)
	{
		JsonObject json = new Gson().fromJson(networkJson, JsonObject.class);
		// Get the network properties
		String name = json.get("name").getAsString();
		String description = json.get("description").getAsString();

		int numInputs = json.get("inputs").getAsInt();
		int numOutputs = json.get("outputs").getAsInt();
		JsonArray layersJson = json.get("layers").getAsJsonArray();
		int[] layers = new int[layersJson.size()];
		for (int i = 0; i < layersJson.size(); i++)
		{
			layers[i] = layersJson.get(i).getAsInt();
		}

		// Create the vanilla network
		MatrixNetwork net = new MatrixNetwork(name, description, numInputs, numOutputs, layers);

		// Overwrite the randomised weight matrices with the trained ones
		JsonArray weightsJson = json.get("weights").getAsJsonArray();
		for (int i = 0; i < weightsJson.size(); i++) {
			JsonArray weightArray = weightsJson.get(i).getAsJsonObject().get("matrix").getAsJsonArray();
			double[][] weights = new double[weightArray.size()][weightArray.get(0).getAsJsonArray().size()];
			for (int j = 0; j < weightArray.size(); j++) {
				for (int k = 0; k < weightArray.get(j).getAsJsonArray().size(); k++) {
					weights[j][k] = weightArray.get(j).getAsJsonArray().get(k).getAsDouble();
				}
				Matrix weightMatrix = new Matrix(weights);
				net.weights.set(i, weightMatrix);
			}
		}
		return net;
	}

	public MatrixNetwork(String name, String description, int numInputs, int numOutputs, int[] layers)
	{
		netName = name;
		netDescription = description;
		layerSizes = Arrays.copyOf(layers, layers.length);

		// Create the input vector
		inputs = new Matrix(numInputs, 1);

		// Create the output and weight vectors for each hidden layer
		int outputSize = numInputs;
        for (int numNeurons : layers) {
			weights.add(Matrix.randomMatrix(numNeurons, outputSize, -1.0, 1.0));
            outputs.add(new Matrix(numNeurons, 1));
            outputSize = numNeurons;
        }

		// Create the output and weight vectors for the output layer
		weights.add(Matrix.randomMatrix(numOutputs, outputSize, -1.0, 1.0));
		outputs.add(new Matrix(numOutputs, 1));

		// Create the output error matrix
		errors = new Matrix(numOutputs, 1);
	}

	public int getOutputCount() {
		return outputs.getLast().rows();
	}

	public int getInputCount() {
		return inputs.rows();
	}

	public double[] getOutputs() {
		return outputs.getLast().column(0);
	}

	public int[] getLayerSizes() {return layerSizes;}

	public double[] getLayerOutputs(int layer) {
		return outputs.get(layer).column(0);
	}

	public void propagate()
	{
		// Propagate inputs
		//processedInputs = inputs.applyFunction(MatrixNetwork::sigmoid).scale(1.0/Math.sqrt(inputs.rows()));
		processedInputs = inputs.scale(1.0/Math.sqrt(inputs.rows()));
		Matrix inputMatrix = new Matrix(processedInputs);

		// Propagate inputs through each layer
		for (int layer=0; layer<weights.size(); layer++) {
			Matrix outputMatrix = weights.get(layer).multiply(inputMatrix).applyFunction(MatrixNetwork::sigmoid);
			outputs.set(layer, outputMatrix);
			inputMatrix = outputMatrix.scale(1.0/Math.sqrt(outputMatrix.rows()));
		}
	}

	public void setInput(int number, double value)
	{
		inputs.setValue(number, 0, value);
	}

	// Allows the setting of an error signal on an output neuron to be backpropagated
	public void setError(int number, double value)
	{
		errors.setValue(number, 0, value);
	}

	/*
	 * Apply backpropagation based on output errors set by the external caller
	 */
	public void teach(double rate)
	{
		Matrix errorMatrix = errors;

		for (int layer=weights.size()-1; layer>=0; layer--) {
			// Get input matrix from previous layer output matrix
			Matrix inputMatrix;
			if (layer == 0)
				inputMatrix = processedInputs.transpose();
			else
				inputMatrix = outputs.get(layer-1).scale(1.0/Math.sqrt(outputs.get(layer-1).rows())).transpose();
			// Calculate output gradients
			Matrix gradientMatrix = outputs.get(layer).applyFunction(MatrixNetwork::sigmoidGradient);
			// Apply gradients to errors to get deltas
			for (int cell=0; cell<errorMatrix.rows(); cell++) {
				double scaledValue = gradientMatrix.getValue(cell, 0) * errorMatrix.getValue(cell, 0);
				errorMatrix.setValue(cell, 0, scaledValue);
			}
			// Calculate weight change matrix
			Matrix weightChange = errorMatrix.multiply(inputMatrix).scale(rate);
			Matrix newWeightMatrix = weights.get(layer).add(weightChange);
			errorMatrix = weights.get(layer).transpose().multiply(errorMatrix);
			weights.set(layer, newWeightMatrix);
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

		json.add("weights", new Gson().toJsonTree(weights));

		return json.toString();
	}

	public static double sigmoid(double value) {
		return 1.0/(1.0+Math.exp(-(value*3.0))); // Sigmoid function compressed to take values between -1 and 1
	}

	public static double sigmoidGradient(double value) {
		return value*(1.0-value)*3.0; // Gradient of vanilla sigmoid is ky(1-y) where k is the "gain"
	}

}
