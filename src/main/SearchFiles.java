package main;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Simple command-line based search demo.
 */
public class SearchFiles {

    private final static int MAX_HITS = 10;

    /**
     *  Interactively enter queries to search for
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println("Usage: SearchFiles [-index INDEX_PATH] \n\n" +
                    "Searches the INDEX_PATH for interactively entered queries");
            System.exit(0);
        }

        String indexPath = "index";
        for (int i = 0; i < args.length; i++)
            if ("-index".equals(args[i]))
                indexPath = args[i + 1];


        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new RoAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);
        while (true) {
            System.out.print("Enter query: ");

            String line = in.readLine().trim();
            if (line.length() == 0) // empty query: exit
                break;

            Query query = parser.parse(line);
            System.out.println("Searching for: " + query.toString("contents"));

            search(searcher, query);
            System.out.println();
        }
        reader.close();
    }

    /**
     * Searches for the given query
     */
    private static void search(IndexSearcher searcher, Query query)
            throws IOException {
        TopDocs results = searcher.search(query, MAX_HITS);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = results.totalHits;
        //System.out.println(numTotalHits + " total matching documents:");

        for (int i = 0; i < numTotalHits; i++) {
            Document doc = searcher.doc(hits[i].doc);
            String path = doc.get("path");
            System.out.println("  " + path);
        }

    }
}