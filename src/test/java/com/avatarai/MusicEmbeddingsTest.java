package com.avatarai;

import com.avatarai.audio.MusicEmbeddingsModel;
import com.avatarai.utils.Feature;
import com.opencsv.CSVReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MusicEmbeddingsTest {
    public static void main(String[] args) throws IOException {
        //testTrainNewModel();
        //testTrainNewModelBig();
        //testGetEmbeddings();
        //testTrainAvatar();
        testAvatarOverTime();
    }

    public static void testAvatarOverTime() throws IOException {
        String content = Files.readString(Path.of("test_files/audio/test_avatar.json"), StandardCharsets.UTF_8);
        MusicEmbeddingsModel model = new MusicEmbeddingsModel("test_files/audio/test_model.json");
        Avatar avatar = new Avatar(content);
        double sampleStart = 0.0;
        double sampleDuration = 30.0;
        double[] embeddings;
        while ((embeddings = model.getDocumentEmbeddings("/home/jon/Music/Brian-Symphony No. 1 in D Minor.1_mp3-to.wav", sampleStart, sampleDuration)) != null) {
            embeddings = Avatar.limitInputRange(embeddings, 0.0);
            double[] outputs = avatar.present(embeddings);
            System.out.println(sampleStart + ", " + outputs[0]);
            sampleStart += sampleDuration;
        }
    }

    public static void testTrainAvatar() throws IOException {
        Avatar avatar = new Avatar("Nathan Bot", "Trained on Nathan's musical preferences", 50, 2, new int[]{50, 50});
        MusicEmbeddingsModel model = new MusicEmbeddingsModel("test_files/audio/test_model.json");
        File dir = new File("test_files/audio/avatar_training");
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".wav"));
        assert files != null;
        HashMap<String, Integer> scores = extractTrainingScores(files);

        ArrayList<double[]> inputSets = new ArrayList<>();
        ArrayList<double[]> outputSets = new ArrayList<>();

        for (File file : files) {
            System.out.println("Getting embeddings for: " + file.getAbsolutePath());
            int score = scores.get(file.getName());
            double[] embeds = model.getDocumentEmbeddings(file.getAbsolutePath());
            embeds = Avatar.limitInputRange(embeds, 0.0);
            inputSets.add(embeds);
            outputSets.add(new double[]{(score-1)*0.25, (5-score)*0.25});
            //outputSets.add(new double[]{(score > 3 ? 1.0 : 0.0), (score < 3 ? 1.0 : 0.0)});
        }

        int reps = 15000;
        int tests = 15;
        for (int rep=0; rep<reps; rep++)
        {
            double netError = 0.0;
            for (int testSet=0; testSet<tests; testSet++)
            {
                double[] result = avatar.train(inputSets.get(testSet), outputSets.get(testSet), 1, 0.001);
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

                StringBuilder msg = new StringBuilder(files[testSet].getName() + ", " + outputSets.get(testSet)[0] + ", " + outputSets.get(testSet)[1]);

                for (double output : result) {
                    msg.append(", ");
                    msg.append(Math.round(100 * output) / 100.0);
                }
                System.out.println(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static HashMap<String, Integer> extractTrainingScores(File[] files) {
        HashMap<String, Integer> scores = new HashMap<>();
        for (File file : files) {
            String filename = file.getName();
            int score = Integer.parseInt(filename.split("\\.")[1]);
            scores.put(filename, score);
        }
        return scores;
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
        MusicEmbeddingsModel.trainEmbeddingsModel("test_files/audio/embeddings_training", "results_files/" + dateStamp + ".musicnet.json", 300);
    }

    public static void testTrainNewModelBig() throws IOException {
        String dateStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        MusicEmbeddingsModel.trainEmbeddingsModel("/home/jon/Music", "results_files/" + dateStamp + ".musicnet.json", 10);
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
