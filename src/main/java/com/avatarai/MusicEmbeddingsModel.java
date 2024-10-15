package com.avatarai;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MusicEmbeddingsModel {
    private static final double SAMPLE_DURATION = 30; // Length in second of the proportion of each music file to sample
    private static final double WORD_LENGTH = 0.25; // Length in seconds of a musical word
    private static final int CONTEXT_WINDOW = 3; // Number of neighbouring samples before and after current sample to predict
    private final Avatar model;

    public MusicEmbeddingsModel(String embeddingsModelFilename) throws IOException {
        String content = Files.readString(Path.of(embeddingsModelFilename), StandardCharsets.UTF_8);
        model = new Avatar(content);
    }

    public double[] getWordEmbeddings(MusicImporter.MusicalWord word) {
        model.present(word.levels());
        return model.getLayerOutputs(0); // Embeddings are encoded in the hidden layer of the network
    }

    public double[] getDocumentEmbeddings(String audioFilename) {
        return getDocumentEmbeddings(audioFilename, 0.0, SAMPLE_DURATION);
    }

    public double[] getDocumentEmbeddings(String audioFilename, double sampleStart, double sampleDuration) {
        File file = new File(audioFilename);
        double[] documentEmbeddings = new double[model.getLayerSizes()[0]];
        ArrayList<MusicImporter.MusicalWord> words = sampleAudioFile(file, sampleStart, sampleDuration);
        if (words.isEmpty())
            return null;

        for (MusicImporter.MusicalWord word : words) {
            double[] embed = getWordEmbeddings(word);
            for (int i = 0; i < embed.length; i++) {
                documentEmbeddings[i] += embed[i] / words.size();
            }
        }
        return documentEmbeddings;
    }

    public static ArrayList<MusicImporter.MusicalWord> sampleAudioFile(File file, double sampleStart, double sampleDuration) {
        ArrayList<MusicImporter.MusicalWord> words = new ArrayList<>();
        AudioInputStream inputStream;
        AudioFormat format;
        try {
            inputStream = AudioSystem.getAudioInputStream(file);
            format = inputStream.getFormat();
            long offset = (int)Math.rint(format.getFrameSize() * sampleStart * format.getSampleRate());
            inputStream.skipNBytes(offset); // Skip forward to requested sample start time
        } catch (Exception e) {
            e.printStackTrace();
            return words;
        }

        int sampleSize = (int)(format.getSampleRate() * WORD_LENGTH);
        sampleSize = Integer.highestOneBit(sampleSize - 1);
        int sampleLength = (int)Math.rint(sampleDuration/WORD_LENGTH);
        MusicImporter.Sample sample;
        int count = 0;
        while ((sample = MusicImporter.readSample(inputStream, sampleSize)) != null && count < sampleLength) {
            count++;
            MusicImporter.Spectrum spectrum = MusicImporter.sampleToSpectrum(sample);
            MusicImporter.MusicalWord word = MusicImporter.tokeniseWord(MusicImporter.spectrumToNotes(spectrum));
            if (word.amplitude() > 0.0) words.add(word); // ignore silence for training purposes
        }
        return words;
    }

    public static void trainEmbeddingsModel(String sourceFileDir, String embeddingsModelFilename) throws IOException {
        int inputs = 120;
        int outputs = 120;

        Avatar model = new Avatar(
                "Music Embeddings",
                "Trained to generate word embeddings for musical words",
                inputs, outputs, new int[]{50});

        ArrayList<double[]> inputSets = new ArrayList<>();
        ArrayList<double[]> outputSets = new ArrayList<>();

        File[] files = getAudioFileList(sourceFileDir);
        for (File file : files) {
            System.out.println("Sampling file: " +file.getAbsolutePath());
            ArrayList<MusicImporter.MusicalWord> words = sampleAudioFile(file, 0.0, SAMPLE_DURATION);
            // Convert list of words into test sets
            System.out.println("Creating test set using " + words.size() + " words");
            for (int i=0; i<words.size()-CONTEXT_WINDOW; i++) {
                //int windowStart = Math.max(i-CONTEXT_WINDOW, 0);
                int windowStart = Math.max(i+1, 0);
                int windowEnd = i+CONTEXT_WINDOW;
                for (int j=windowStart; j<=windowEnd ; j++) {
                    if (j != i) {
                        inputSets.add(words.get(i).levels());
                        outputSets.add(words.get(j).levels());
                    }
                }
            }
            System.out.println("Test set size is now " + inputSets.size());
        }

        // Now train the model based on all the input and output pairs
        System.out.println("Training the embeddings model");

        int reps = 30;
        for (int rep=0; rep<reps; rep++) {
            double netError = 0.0;
            int tests = inputSets.size();
            for (int testSet = 0; testSet < tests; testSet++) {
                double[] result = model.train(inputSets.get(testSet), outputSets.get(testSet), 1, 0.001);
                double error = 0.0;
                for (int i = 0; i < result.length; i++) {
                    error += Math.pow(outputSets.get(testSet)[i] - result[i], 2);
                }
                netError += Math.sqrt(error);
            }

            //if (rep % 10 == 0)
                System.out.println(System.currentTimeMillis() + "ms: " + rep + " of " + reps + ", err = " + netError / tests);
        }

        FileWriter netWriter = new FileWriter(embeddingsModelFilename);
        netWriter.write(model.toString());
        netWriter.flush();
        netWriter.close();

        for (int testSet=0; testSet<inputSets.size(); testSet++)
        {
            double[] result = model.present(inputSets.get(testSet));
            double similarity = 0.0;
            double magnitude1 = 0.0, magnitude2 = 0.0;
            for (int i = 0; i < result.length; i++) {
                similarity += outputSets.get(testSet)[i] * result[i];
                magnitude1 += Math.pow(outputSets.get(testSet)[i], 2);
                magnitude2 += Math.pow(result[i], 2);
            }
            String inputString = arrayToString(inputSets.get(testSet));
            String resultString = arrayToString(result);
            String expectedString = arrayToString(outputSets.get(testSet));
            magnitude1 = Math.sqrt(magnitude1);
            magnitude2 = Math.sqrt(magnitude2);
            similarity /= (magnitude1 * magnitude2);
            System.out.println(similarity);
            System.out.println(inputString);
            System.out.println(expectedString);
            System.out.println(resultString);
        }
    }

    private static String arrayToString(double[] array) {
        StringBuilder sb = new StringBuilder();
        for (double v : array) {
            sb.append((v > 0.333 ? "X" : "_"));
        }
        return sb.toString();
    }

    private static File[] getAudioFileList(String testPath) {
        File dir = new File(testPath);
        return dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".wav"));
    }
}
