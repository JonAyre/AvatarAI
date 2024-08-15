package com.avatarai;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;

@SuppressWarnings("CallToPrintStackTrace")
public class HaceTest {

	public record Article(Entity entity, String sentiment) {}
	public record Document(Entity entity, String rating) {}

	public static void main(String[] args)
	{
//		testLoadArticles();
//		loadArticleEmbeds("articleEmbeds.csv");
//		docsToEmbeddings("test_files/scraped_documents", "test_files/documentEmbeds.csv");
// 		docsToEmbeddings("test_files/scraped_guidance", "test_files/guidanceEmbeds.csv");
//		docsToEmbeddings("test_files/scraped_articles", "test_files/articleEmbeds.csv");
//		docsToEmbeddings("temp", "temp/articleEmbeds.csv");
		testScoreArticles();
//		testScoreDocs();
//		testCompareDocs();
//		convertArticles();

	}

	public static void testCompareDocs()
	{
		Vector<Document> docs = loadDocuments(50);
		Vector<Document> guidanceDocs = loadGuidanceDocuments(50);
		for (Document doc : docs)
		{
			String msg = "";
			Feature docFeature = doc.entity().getFeature("Content");
			double totalMatch = 0.0;
			for (Document guidance : guidanceDocs)
			{
				Feature guidanceFeature = guidance.entity().getFeature("Content");
				double match = docFeature.compare(guidanceFeature);
				msg += Math.round(100 * match) / 100.0 + ", ";
				totalMatch += match;
			}
			msg += Math.round(100 * totalMatch) / 100.0 + ", ";
			msg += doc.rating() + ", " + doc.entity().getId();
			System.out.println(msg);
		}

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

	public static void testScoreDocs()
	{
		int inputs = 50;

		Vector<Document> docs = loadDocuments(inputs);
		Vector<Document> goodDocuments = new Vector<>();
		Vector<Document> poorDocuments = new Vector<>();
		Vector<Document> otherDocuments = new Vector<>();
		Vector<Document> guidance = loadGuidanceDocuments(inputs);

		for (Document doc: docs)
		{
			if (doc.rating.contains("poor"))
				poorDocuments.add(doc);
			else if (doc.rating.contains("good"))
				goodDocuments.add(doc);
			else
				otherDocuments.add(doc);
		}

		Avatar net = new Avatar("Governance Bot", "Trained to identify if documents are good or bad from a governance scoring perspective. Output 0 = good, output 1 = bad", inputs, 2, 50, 1);

		ArrayList<double[]> inputSets = new ArrayList<>();
		ArrayList<double[]> outputSets = new ArrayList<>();

		// Set up the training cases by taking a document from each list in turn until we run out
		int longestList = Math.max(guidance.size(), Math.max(goodDocuments.size(), Math.max(poorDocuments.size(), otherDocuments.size())));
		for (int i=0; i<longestList; i++)
		{
			if (i < guidance.size())
			{
				inputSets.add(guidance.get(i).entity().getFeature("Content").getValues());
				outputSets.add(new double[]{1.0, 0.0});
			}
			if (i < poorDocuments.size())
			{
				inputSets.add(poorDocuments.get(i).entity().getFeature("Content").getValues());
				outputSets.add(new double[]{0.0, 1.0});
			}
			if (i < goodDocuments.size())
			{
				inputSets.add(goodDocuments.get(i).entity().getFeature("Content").getValues());
				outputSets.add(new double[]{1.0, 0.0});
			}
			if (i < otherDocuments.size())
			{
				inputSets.add(otherDocuments.get(i).entity().getFeature("Content").getValues());
				outputSets.add(new double[]{0.5, 0.5});
			}
		}

		for (int rep=0; rep<2000; rep++)
		{
			double netError = 0.0;

			for (int testSet=0; testSet<=12; testSet++)
			{
				double[] result = net.train(inputSets.get(testSet), outputSets.get(testSet), 5, 0.01);

				double error = 0.0;
				for (int i=0; i<result.length; i++)
				{
					error += Math.pow(outputSets.get(testSet)[i] - result[i], 2);
				}
				netError += Math.sqrt(error);
			}

			if (rep%10 == 0)
				System.out.println(rep + ", " + netError/12);
		}

		// Now recheck to ensure that the learning has "stuck"
		String dateStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		//FileWriter results;
		FileWriter netWriter;

		try {
			netWriter = new FileWriter("results_files/" + dateStamp + ".docnet.json");
			netWriter.write(net.toString());
			netWriter.flush();
			netWriter.close();

//			results = new FileWriter("results_files/" + dateStamp + ".docresults.csv");
//			results.write("Expected score, Pos score, Neg score, Article URL\n");

			for (int testSet=0; testSet<inputSets.size(); testSet++)
			{
				double[] result = net.present(inputSets.get(testSet));

				StringBuilder msg = new StringBuilder(outputSets.get(testSet)[0] + ", " + outputSets.get(testSet)[1] + " : ");

				for (int out=0; out<result.length; out++)
				{
					double output = result[out];
					msg.append(Math.round(100 * output) / 100.0).append(", ");
				}

				//System.out.println(msg + doc.entity().getId());
				System.out.println(msg);

				// Also save result in output file
				//results.write(doc.rating() + ", " + Math.round(100*negnet.getOutput(0))/100.0 + ", " + Math.round(100*posnet.getOutput(0))/100.0 + ", " + doc.entity().getId() + "\n");
			}
			//results.flush();
			//results.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void testScoreArticles()
	{
		int inputs = 200;
		Vector<Article> articles = loadArticles(0);
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
				inputs, 2, 50, 2);

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

		for (int i=0; i<neutralArticles.size(); i++)
		{
			inputSets.add(neutralArticles.get(i).entity().getFeature("Content").getValues());
			outputSets.add(new double[]{0.0, 0.0});
			articleSet.add(neutralArticles.get(i));
		}
		for (int i=0; i<unscoredArticles.size(); i++)
		{
			inputSets.add(unscoredArticles.get(i).entity().getFeature("Content").getValues());
			outputSets.add(new double[]{0.0, 0.0});
			articleSet.add(unscoredArticles.get(i));
		}

		for (int rep=0; rep<1500; rep++)
		{
			double netError = 0.0;
			for (int testSet=0; testSet<=50; testSet++)
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
				System.out.println(rep + ", " + netError/16);
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

				for (int out=0; out<result.length; out++)
				{
					double output = result[out];
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

	public static void testLoadArticles()
	{
		Vector<Article> articles = loadArticles(0);
        for (Article art1 : articles) {
            Entity ent1 = art1.entity();
            System.out.printf("%-20.20s", ent1.getId());
            for (Article art2 : articles) {
                Entity ent2 = art2.entity();
                double match = ent1.compare(ent2);
                System.out.printf(", %4.2f", match);
            }
            System.out.println();
        }
	}

	public static void convertArticles()
	{
		String filePath = "temp";
		Set<String> files = Stream.of(Objects.requireNonNull(new File(filePath).listFiles()))
				.filter(file -> !file.isDirectory())
				.map(File::getName)
				.collect(Collectors.toSet());

		try {
			HashMap<String, String> articles = new HashMap<>();
			Reader reader = new FileReader("test_files/articleScores.csv");
			CSVReader csvReader = new CSVReader(reader);
			List<String[]> rows = csvReader.readAll();
			rows.removeFirst(); // Remove header row
			for (String[] row: rows)
			{
				if (!row[2].isEmpty())
					articles.put(row[2], row[3]); // ID, URL
			}
			for (String file : files)
			{
				String id = file.substring(0, file.lastIndexOf('.')); // filename without .txt
				if (articles.containsKey(id)) {
					FileWriter outFile = new FileWriter(id + ".json");
					String content = Files.readString(Path.of(filePath + '/' + file));
					System.out.println("Converting: " + file);
					JsonObject output = new JsonObject();
					output.addProperty("content", content);
					output.addProperty("url", articles.get(id));
					outFile.write(output.toString());
					outFile.flush();
					outFile.close();
				}
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
			rows.remove(0); // Remove header row
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

	public static Vector<Article> loadArticles(int limit) {
		HashMap<String, String> sentiments = loadArticleSentiments("test_files/articleScores.csv");
		HashMap<String, String> embeds = loadEmbeds("test_files/articleEmbeds.csv");

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

	public static HashMap<String, String> loadDocumentScores(String filename)
	{
		HashMap<String, String> scores = new HashMap<>();
		try {
			Reader reader = new FileReader(filename);
			CSVReader csvReader = new CSVReader(reader);
			List<String[]> rows = csvReader.readAll();
			rows.remove(0); // Remove header row
			for (String[] row: rows)
			{
				if (!row[1].isEmpty())
					scores.put(row[1], row[3]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return scores;
	}

	public static Vector<Document> loadDocuments(int length) {
		HashMap<String, String> scores = loadDocumentScores("test_files/documentScores.csv");
		HashMap<String, String> embeds = loadEmbeds("test_files/documentEmbeds.csv");

		Vector<Document> docs = new Vector<>();

		for (Map.Entry<String, String> embed : embeds.entrySet())
		{
			Entity entity = new Entity("Document", embed.getKey());
			double[] values;
			String score = scores.get(entity.getId()).toLowerCase();
			String valueString = embed.getValue();
			values = Stream.of(valueString.substring(1, valueString.length()-1).split(",")).mapToDouble(Double::parseDouble).toArray();
			Feature feature = new Feature(values);
			if (length > 0)
			{
				feature.truncate(length);
				feature.normalise();
			}
			entity.addFeature("Content", feature);
			Document doc = new Document(entity, score);
			docs.add(doc);
		}

		return docs;
	}

	public static Vector<Document> loadGuidanceDocuments(int length) {
		HashMap<String, String> embeds = loadEmbeds("test_files/guidanceEmbeds.csv");

		Vector<Document> docs = new Vector<>();

		for (Map.Entry<String, String> embed : embeds.entrySet())
		{
			Entity entity = new Entity("Document", embed.getKey());
			double[] values;
			String score = "guidance";
			String valueString = embed.getValue();
			values = Stream.of(valueString.substring(1, valueString.length()-1).split(",")).mapToDouble(Double::parseDouble).toArray();
			Feature feature = new Feature(values);
			if (length > 0)
			{
				feature.truncate(length);
				feature.normalise();
			}
			entity.addFeature("Content", feature);
			Document doc = new Document(entity, score);
			docs.add(doc);
		}

		return docs;
	}

}
