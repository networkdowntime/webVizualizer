package net.networkdowntime.javaAnalyzer.search;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class Search {
	private Directory directory = new RAMDirectory();
	private IndexWriter indexWriter = null;
	
	private void setup() {
		if (indexWriter == null) {
			try {
				Analyzer analyzer = new StandardAnalyzer();
				IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND); // package create  Represents the creation or appended to the existing index database  
				indexWriter = new IndexWriter(directory, iwc); // to write the document to the index database  
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addDocument(String packageName, String className, String text) {
		long startTime = new Date().getTime();
		setup();

		Document document;

		document = new Document();

		Field packagePath = new StoredField("packageName", packageName);
		Field classPath = new StoredField("className", className);
		document.add(packagePath);
		document.add(classPath);

		Field FieldBody = new TextField("body", TextScrubber.scrubToString(text), Store.YES);
		document.add(FieldBody);

		try {
			indexWriter.addDocument(document);
		} catch (IOException e) {
			e.printStackTrace();
		}

		long endTime = new Date().getTime();
		System.out.println("  spent  " + (endTime - startTime) + "  milliseconds to add " + className + " the search index");

	}

	public Map<String, Float> query(String queryString, int topNResults, boolean classesNotPackages) {
		Map<String, Float> searchResults = new HashMap<String, Float>();
		IndexSearcher searcher;

		try {
			IndexReader reader = DirectoryReader.open(directory);
			searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();
			
			Query query = new QueryParser("body", analyzer).parse(queryString);
			TopDocs results = searcher.search(query, topNResults);
			ScoreDoc[] hits = results.scoreDocs;

			for (ScoreDoc scoreDoc : hits) {
				Document doc = searcher.doc(scoreDoc.doc);
				float score = scoreDoc.score;
				String name = doc.get( (classesNotPackages) ? "className" : "packageName" );
				
				if (searchResults.containsKey(name)) {
					searchResults.put(name, searchResults.get(name) + score);
				} else {
					searchResults.put(name, score);
				}

			}

			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return searchResults;
	}

	public void finishedIndexing() {
		try {
			indexWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		indexWriter = null;
	}
}
