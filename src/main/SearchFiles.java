package main;

import com.sun.deploy.util.StringUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Simple command-line based search demo.
 */
public class SearchFiles {

    private final static int MAX_HITS = 10;
    private final static int MAX_N_FRAGMENTS = 5;
    private final static int MAX_FRAGMENT_SIZE = 30;
    private final static HashSet<String> DOC_TYPES = new HashSet<>(Arrays.asList("doc", "docx", "txt", "pdf", "html" ));

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

            String[] splitWords = line.split("\\s"); // split by whitespace
            HashSet<String> searchedExtensions = new HashSet<>();
            for (String word : splitWords)
                if (DOC_TYPES.contains(word)) // if it is an allowed extension
                    searchedExtensions.add(word);

            String numberOnly = line.replaceAll("[^0-9]", ""); //removes all non-digits

            if (numberOnly.length() > 0) {
                Date inputDate = inputFormatToDate(numberOnly);
                System.out.println("Looking for files from: " + dateToOutputFormat(inputDate));
            }


            Query query = parser.parse(line.replaceAll("[^a-zA-Z]", "")); //removes all non alphabetic characters
            System.out.println("Searching for: " + query.toString("contents"));
            System.out.println();

            search(searcher, query, searchedExtensions, numberOnly);
        }

        reader.close();
    }


    /**
     * Search for the given query
     */
    private static void search(IndexSearcher searcher, Query query,
                               Set<String> searchedExtensions, String searchedDateString)
            throws IOException, InvalidTokenOffsetsException, ParseException {

        QueryScorer scorer = new QueryScorer(query);
        Formatter formatter = new  SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, MAX_FRAGMENT_SIZE);
        highlighter.setTextFragmenter(fragmenter);

        TopDocs results = searcher.search(query, MAX_HITS);
        ScoreDoc[] hits = results.scoreDocs;

        if (searchedExtensions.size() > 0) {
            System.out.println("Se cauta numai in fisiere de tipul: " + String.join(", ", searchedExtensions));
        }

        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);

            String content = doc.get("contents");
            TokenStream stream = TokenSources.getTokenStream("contents", content, new RoAnalyzer());
            String[] fragments = highlighter.getBestFragments(stream, content, MAX_N_FRAGMENTS);

            String path = doc.get("path");
            String extension = FilenameUtils.getExtension(path);
            // if the  file's extension is not among the ones searched for, ignore this document
            if (!isValidExtension(searchedExtensions, extension))
                continue;


            // Get modified date of current document
            String docDateString = doc.get("modified");
            // Skip if the document's date is before the searched date
            if (!isValidDate(docDateString, searchedDateString))
                continue;

            Date docDate = luceneFormatToDate(docDateString);
            System.out.println(">" + path + "  |  modified: " + dateToOutputFormat(docDate));

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
            }

            System.out.println();
            System.out.println(StringUtils.join(filteredFragments, "..."));

        }

    }

    private static boolean isValidExtension(Set<String> validExtensions, String extension) {
        if (validExtensions.size() == 0)
            return true;
        return validExtensions.contains(extension);
    }

    private static Date luceneFormatToDate(String string) throws ParseException {
        DateFormat luceneFormat = new SimpleDateFormat("yyyyMMddHHmm");
        return luceneFormat.parse(string);
    }

    private static Date inputFormatToDate(String string) throws ParseException {
        DateFormat inputFormat = new SimpleDateFormat("ddMMyyyy");
        return inputFormat.parse(string);
    }

    private static String dateToOutputFormat(Date date) {
        DateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        return outputFormat.format(date);
    }


    private static boolean isValidDate(String docDateString, String searchedDateString) throws ParseException {
        if (searchedDateString.isEmpty()) // no special format requested, any date is valid
            return true;

        Date docDate = luceneFormatToDate(docDateString);
        Date searchedDate = inputFormatToDate(searchedDateString);

        // only documents edited later than the searched date are valid
        return docDate.after(searchedDate);
    }
}

