package com.avatarai;

import com.opencsv.CSVReader;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MusicEmbeddingsTest {
    public static void main(String[] args) throws IOException {
        //testTrainNewModel();
        //testGetEmbeddings();
        testTrainAvatar();
    }

    public static void testTrainAvatar() throws IOException {
        Avatar avatar = new Avatar("Nathan Bot", "Trained on Nathan's musical preferences", 50, 2, new int[]{50, 10});
        MusicEmbeddingsModel model = new MusicEmbeddingsModel("test_files/audio/test_model.json");
        File dir = new File("test_files/audio/avatar_training");
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".wav"));
        HashMap<String, Integer> scores = loadTrainingScores("test_files/audio/avatar_training/scores.csv");

        ArrayList<double[]> inputSets = new ArrayList<>();
        ArrayList<double[]> outputSets = new ArrayList<>();

        assert files != null;
        for (File file : files) {
            System.out.println("Getting embeddings for: " + file.getAbsolutePath());
            int score = scores.get(file.getName());
            double[] embeds = model.getDocumentEmbeddings(file.getAbsolutePath());
            inputSets.add(embeds);
            outputSets.add(new double[]{(score > 3 ? 1.0 : 0.0), (score < 3 ? 1.0 : 0.0)});
        }

        int reps = 2000;
        int tests = inputSets.size();
        for (int rep=0; rep<reps; rep++)
        {
            double netError = 0.0;
            for (int testSet=0; testSet<tests; testSet++)
            {
                double[] result = avatar.train(inputSets.get(testSet), outputSets.get(testSet), 1, 0.1);
                double error = 0.0;
                for (int i=0; i<result.length; i++)
                {
                    error += Math.pow(outputSets.get(testSet)[i] - result[i], 2);
                }
                netError += Math.sqrt(error);
            }

            if (rep%100 == 0)
                System.out.println(rep + " of " + reps + ", err = " + netError/tests);
        }

        // Now recheck to ensure that the learning has "stuck"
        String dateStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        FileWriter netWriter;

        try {
            netWriter = new FileWriter("results_files/" + dateStamp + ".music-avatar.json");
            netWriter.write(avatar.toString());
            netWriter.flush();
            netWriter.close();

            for (int testSet=0; testSet<inputSets.size(); testSet++)
            {
                double[] result = avatar.present(inputSets.get(testSet));

                StringBuilder msg = new StringBuilder(outputSets.get(testSet)[0] + ", " + outputSets.get(testSet)[1] + ": ");

                for (double output : result) {
                    msg.append(Math.round(100 * output) / 100.0).append(", ");
                }
                System.out.println(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static HashMap<String, Integer> loadTrainingScores(String filename) {
        HashMap<String, Integer> scores = new HashMap<>();
        try {
            Reader reader = new FileReader(filename);
            CSVReader csvReader = new CSVReader(reader);
            List<String[]> rows = csvReader.readAll();
            rows.removeFirst(); // Remove header row
            for (String[] row: rows)
            {
                scores.put(row[0], Integer.parseInt(row[1].trim()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scores;
    }

    public static void testTrainNewModel() throws IOException {
        String dateStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        MusicEmbeddingsModel.trainEmbeddingsModel("test_files/audio/embeddings_training", "results_files/" + dateStamp + ".musicnet.json");
    }

    public static void testGetEmbeddings() throws IOException {
        MusicEmbeddingsModel model = new MusicEmbeddingsModel("test_files/audio/test_model.json");
        File dir = new File("test_files/audio/embeddings_training");
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".wav"));
        assert files != null;
        ArrayList<Feature> features = new ArrayList<>();
        for (File file : files) {
            System.out.println("Sampling file: " +file.getAbsolutePath());
            Feature feature = new Feature(model.getDocumentEmbeddings(file.getAbsolutePath()));
            features.add(feature);
        }

        for (Feature feature1 : features) {
            for (Feature feature2 : features) {
                double similarity = feature1.compare(feature2);
                System.out.print(similarity + ", ");
            }
            System.out.println();
        }
    }
}
