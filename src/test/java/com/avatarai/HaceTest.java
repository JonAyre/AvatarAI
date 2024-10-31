package com.avatarai;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;
import java.util.List;

import com.avatarai.text.TextImporter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.opencsv.CSVReader;

@SuppressWarnings("CallToPrintStackTrace")
public class HaceTest {

	public record Article(Entity entity, String sentiment) {}

	public static void main(String[] args)
	{
//		docsToEmbeddings("test_files/scraped_articles", "test_files/articleEmbeds.csv");
		testScoreArticles();
	}

	public static void docsToEmbeddings(String inputPath, String outputFile)
	{
		File dir = new File(inputPath);
		File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".json"));
		if (files == null) return;
		try {
			FileWriter outFile = new FileWriter(outputFile);
            for (File file : files)
			{
				String content = Files.readString(file.toPath());
				JsonObject json = new Gson().fromJson(content, JsonObject.class);
				System.out.println(file);
				double[] values = TextImporter.getEmbeddings(json.get("content").getAsString());
				String id = json.get("url").getAsString();
				outFile.write(id + "," + "\"" + Arrays.toString(values) + "\"\n");
			}
			outFile.flush();
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void testScoreArticles()
	{
		int inputs = 100;
		Vector<Article> articles = loadArticles("test_files/articleScores.csv", "test_files/articleEmbeds.csv", 0);
		Vector<Article> positiveArticles = new Vector<>();
		Vector<Article> neutralArticles = new Vector<>();
		Vector<Article> negativeArticles = new Vector<>();
		Vector<Article> unscoredArticles = new Vector<>();

		for (Article article: articles)
		{
			if (article.sentiment.contains("positive"))
				positiveArticles.add(article);
			else if (article.sentiment.contains("negative"))
				negativeArticles.add(article);
			else if (article.sentiment.contains("neutral"))
				neutralArticles.add(article);
			else
				unscoredArticles.add(article);
		}

		Avatar net = new Avatar(
				"Sentiment Bot",
				"Trained to rate articles positive or negative sentiment. Output 0 = positive, Output 1 = negative",
				inputs, 2, new int[]{100, 50, 20});

		ArrayList<double[]> inputSets = new ArrayList<>();
		ArrayList<double[]> outputSets = new ArrayList<>();
		ArrayList<Article> articleSet = new ArrayList<>();

		// Set up the training cases by taking a document from each list in turn until we run out
		int longestList = Math.max(positiveArticles.size(), negativeArticles.size());
		for (int i=0; i<longestList; i++) {
			if (i < positiveArticles.size()) {
				inputSets.add(positiveArticles.get(i).entity().getFeature("Content").getValues());
				outputSets.add(new double[]{1.0, 0.0});
				articleSet.add(positiveArticles.get(i));
			}
			if (i < negativeArticles.size()) {
				inputSets.add(negativeArticles.get(i).entity().getFeature("Content").getValues());
				outputSets.add(new double[]{0.0, 1.0});
				articleSet.add(negativeArticles.get(i));
			}
		}

        for (Article neutralArticle : neutralArticles) {
            inputSets.add(neutralArticle.entity().getFeature("Content").getValues());
            outputSets.add(new double[]{0.0, 0.0});
            articleSet.add(neutralArticle);
        }
        for (Article unscoredArticle : unscoredArticles) {
            inputSets.add(unscoredArticle.entity().getFeature("Content").getValues());
            outputSets.add(new double[]{0.0, 0.0});
            articleSet.add(unscoredArticle);
        }
		int reps = 1500;
		for (int rep=0; rep<reps; rep++)
		{
			double netError = 0.0;
			int tests = 80;
			for (int testSet=0; testSet<=tests; testSet++)
			{
				double[] result = net.train(inputSets.get(testSet), outputSets.get(testSet), 1, 0.01);
				double error = 0.0;
				for (int i=0; i<result.length; i++)
				{
					error += Math.pow(outputSets.get(testSet)[i] - result[i], 2);
				}
				netError += Math.sqrt(error);
			}

			if (rep%10 == 0)
				System.out.println(rep + " of " + reps + ", err = " + netError/tests);
		}
		
		System.out.println("=========================================================");

		// Now recheck to ensure that the learning has "stuck"
		String dateStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		FileWriter results;
		FileWriter netWriter;

		try {
			netWriter = new FileWriter("results_files/" + dateStamp + ".artnet.json");
			netWriter.write(net.toString());
			netWriter.flush();
			netWriter.close();

			results = new FileWriter("results_files/" + dateStamp + ".artresults.csv");
			results.write("Sentiment, Positive score, Negative score, Article URL\n");

			for (int testSet=0; testSet<inputSets.size(); testSet++)
			{
				double[] result = net.present(inputSets.get(testSet));

				StringBuilder msg = new StringBuilder(articleSet.get(testSet).sentiment() + ", ");

                for (double output : result) {
                    msg.append(Math.round(100 * output) / 100.0).append(", ");
                }
				msg.append(articleSet.get(testSet).entity().getId());
				System.out.println(msg);

				// Also save result in output file
				results.write(msg + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static HashMap<String, String> loadArticleSentiments(String filename)
	{
		HashMap<String, String> sentiments = new HashMap<>();
		try {
			Reader reader = new FileReader(filename);
			CSVReader csvReader = new CSVReader(reader);
			List<String[]> rows = csvReader.readAll();
			rows.removeFirst(); // Remove header row
			for (String[] row: rows)
			{
				if (!row[3].isEmpty())
					sentiments.put(row[3], row[5]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sentiments;
	}

	public static HashMap<String, String> loadEmbeds(String filename)
	{
		HashMap<String, String> embeds = new HashMap<>();
		try {
			Reader reader = new FileReader(filename);
			CSVReader csvReader = new CSVReader(reader);
			List<String[]> rows = csvReader.readAll();
			for (String[] row: rows)
			{
				embeds.put(row[0], row[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return embeds;
	}

	public static Vector<Article> loadArticles(String scoresFile, String embedsFile, int limit) {
		HashMap<String, String> sentiments = loadArticleSentiments(scoresFile);
		HashMap<String, String> embeds = loadEmbeds(embedsFile);

		Vector<Article> articles = new Vector<>();

		for (Map.Entry<String, String> embed : embeds.entrySet())
		{
			Entity entity = new Entity("Article", embed.getKey());
			double[] values;
			String sentiment = sentiments.get(entity.getId());
			if (sentiment == null) sentiment = "<none>";

			String valueString = embed.getValue();
			values = Stream.of(valueString.substring(1, valueString.length()-1).split(",")).mapToDouble(Double::parseDouble).toArray();
			Feature feature = new Feature(values);
			if (limit > 0)
			{
				feature.truncate(limit);
				feature.normalise();
			}
			entity.addFeature("Content", feature);
			Article article = new Article(entity, sentiment);
			articles.add(article);
		}

        return articles;
	}
}
