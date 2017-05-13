package main;

import com.sun.deploy.util.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Simple command-line based search demo.
 */
public class SearchFiles {

    private final static int MAX_HITS = 10;
    private final static int MAX_N_FRAGMENTS = 5;
    private final static int MAX_FRAGMENT_SIZE = 30;

    public SearchFiles() {
    }

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
            System.out.println();

            search(searcher, query);
//            System.out.println();
        }


        reader.close();
    }

    // matematica litere

    /**
     * Searches for the given query
     */
    private static void search(IndexSearcher searcher, Query query)
            throws IOException, InvalidTokenOffsetsException {

        QueryScorer scorer = new QueryScorer(query);
        Formatter formatter = new  SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, MAX_FRAGMENT_SIZE);
        highlighter.setTextFragmenter(fragmenter);

        TopDocs results = searcher.search(query, MAX_HITS);
        ScoreDoc[] hits = results.scoreDocs;

        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);

            String content = doc.get("contents");
            TokenStream stream = TokenSources.getTokenStream("contents", content, new RoAnalyzer());
//            String fragments = highlighter.getBestFragments(stream, content, 1, "...");
            String[] fragments = highlighter.getBestFragments(stream, content, MAX_N_FRAGMENTS);

            String path = doc.get("path");
            System.out.println(">" + path);

            ArrayList<String> filteredFragments = new ArrayList<>();

            HashSet<String> alreadyShown = new HashSet<>();
            for (String fragment : fragments) {
                int indexOfTerm = fragment.indexOf("<B>") + 3; // length of <B>
                String term = fragment.substring(indexOfTerm, indexOfTerm + 3); // first 3 letters define the term (psedo-stemming)
                term = term.toLowerCase();

                if (alreadyShown.contains(term)) // already shown a fragment for this term, skip
                    continue;
                alreadyShown.add(term);

                filteredFragments.add(fragment);
//                System.out.println();
//                System.out.println("  " + fragment);
            }
            System.out.println();
            System.out.println(StringUtils.join(filteredFragments, "..."));

        }

    }
}