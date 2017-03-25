package main;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.tartarus.snowball.ext.RomanianStemmer;

import java.util.Arrays;

class RoAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer source = new StandardTokenizer();
        TokenStream filter = new StandardFilter(source);

        CharArraySet stopwords = RomanianAnalyzer.getDefaultStopSet();
        stopwords.addAll(Arrays.asList("si", "in", "la"));

         // Order of filters
        filter = new LowerCaseFilter(filter);
        filter = new ASCIIFoldingFilter(filter);
        filter = new StopFilter(filter, stopwords);
        filter = new SnowballFilter(filter, new RomanianStemmer());

        return new TokenStreamComponents(source, filter);
    }
}
