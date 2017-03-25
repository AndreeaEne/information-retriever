package main;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.tartarus.snowball.ext.RomanianStemmer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class RoAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer source = new StandardTokenizer();
        TokenStream filter = new StandardFilter(source);

        CharArraySet stopwords = RomanianAnalyzer.getDefaultStopSet();

        List<String> list = null;
        try {
            list = Files.readAllLines(Paths.get("info/stopwords-ro.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] arrayWords = list.toArray(new String[list.size()]);
        Collection allWords = new ArrayList(Arrays.asList(arrayWords));

        stopwords.addAll(allWords);

         // Filters are ordered
        filter = new LowerCaseFilter(filter);
        filter = new ASCIIFoldingFilter(filter);
        filter = new StopFilter(filter, stopwords);
        filter = new SnowballFilter(filter, new RomanianStemmer());

        return new TokenStreamComponents(source, filter);
    }
}
