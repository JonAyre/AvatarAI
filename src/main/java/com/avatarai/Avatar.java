package com.avatarai;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class Avatar
{
    private final Network network;

    public Avatar(String savedAvatar) {
        network = Network.fromString(savedAvatar);
    }

    public Avatar(String name, String description, int inputs, int outputs, int width, int hiddenLayers)
    {
        int[] layers = new int[hiddenLayers+1];
        Arrays.fill(layers, width);
        network = new Network(name, description, inputs, outputs, layers);
    }

    public Avatar(String name, String description, int inputs, int outputs, int[] layers)
    {
        network = new Network(name, description, inputs, outputs, layers);
    }

    public double[] present(double[] inputSet)
    {
        double[] outputSet = new double[network.getOutputCount()];

        // Set the network inputs to the presented values
        for (int input=0; input<network.getInputCount(); input++)
        {
            double value = (input<inputSet.length) ? inputSet[input] : 0.0;
            network.setInput(input, value);
        }

        // Propagate the inputs through the network to the outputs
        network.propagate();

        // Collect the resulting outputs
        outputSet = network.getOutputs();

        return outputSet;
    }

    // Train the network with a given input set and desired output set
    // Repeat for the specified number of cycles and train at the defined rate
    // Typical rates for successful learning are between 0.001 and 0.01 for 5 cycles
    // Returns the resulting output set to allow progress to be measured/reported
    public double[] train(double[] inputSet, double[] outputSet, int cycles, double rate)
    {
        // Set the network inputs to the presented values
        for (int input=0; input<network.getInputCount(); input++)
        {
            double value = (input<inputSet.length) ? inputSet[input] : 0.0;
            network.setInput(input, value);
        }

        // Teach the network for the desired number of training cycles
        for (int cycle=0; cycle<cycles; cycle++)
        {
            // Propagate the inputs through the network to the outputs
            network.propagate();

            // Calculate the output errors and feed them back in to the outputs
            double[] actualOutputs = network.getOutputs();
            for (int output=0; output<Math.min(actualOutputs.length, outputSet.length); output++)
            {
                double error = outputSet[output]-actualOutputs[output];
                network.setError(output, error);
            }

            // Teach the network at the desired training rate
            network.teach(rate);
        }

        // Return the final resulting outputs of the network
        // Collect the resulting outputs
        return network.getOutputs();
    }

    public double[] getLayerOutputs(int layer) {
        return network.getLayerOutputs(layer);
    }

    public int[] getLayerSizes() {
        return network.getLayerSizes();
    }

    public String toString()
    {
        return network.toString();
    }
}
