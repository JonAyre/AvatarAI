package com.avatarai;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MusicEmbeddingsTest {
    public static void main(String[] args) throws IOException {
        //testTrainNewModel();
        testGetEmbeddings();
    }

    public static void testTrainNewModel() throws IOException {
        String dateStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        MusicEmbeddingsModel.trainEmbeddingsModel("test_files/audio/embeddings_training", "results_files/" + dateStamp + ".musicnet.json");
    }

    public static void testGetEmbeddings() throws IOException {
        MusicEmbeddingsModel model = new MusicEmbeddingsModel("test_files/audio/test_model.json");
        File dir = new File("test_files/audio/avatar_training");
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
