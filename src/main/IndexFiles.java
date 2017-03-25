package main;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

/**
 * Index all text files under a directory.
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {

    /**
     * Index all text files under a directory.
     */
    public static void main(String[] args)
            throws TikaException, SAXException, IOException {

        // Parse arguments
        String indexPath = "index";
        String docsPath = "Docs/";
        for (int i = 0; i < args.length; i++)
            if ("-index".equals(args[i])) {
                indexPath = args[i + 1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i + 1];
                i++;
            }

        if (docsPath == null) {
            System.err.println("Usage: IndexFiles [-index INDEX_PATH] [-docs DOCS_PATH] \n\n"
                    + "Indexes the documents in DOCS_PATH, creating a Lucene index"
                    + "in INDEX_PATH that can be searched with SearchFiles");
            System.exit(1);
        }

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" + docDir.toAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        System.out.println("Indexing to directory '" + indexPath + "'...");

        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new RoAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);

        IndexWriter writer = new IndexWriter(dir, iwc);
        indexDocs(writer, docDir);

        writer.close();

        System.out.println("Duration: " + (new Date().getTime() - start.getTime()) / 1000 + " seconds");
    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     * NOTE: This method indexes one document per input file.  This is slow.  For good
     * throughput, put multiple documents into your input file(s).  An example of this is
     * in the benchmark module, which can create "line doc" files, one document per line,
     * using the
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param path   The file to index, or the directory to recurse into to find files to index
     * @throws IOException If there is a low-level I/O error
     */
    private static void indexDocs(final IndexWriter writer, Path path)
            throws IOException, TikaException, SAXException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file);
                    } catch (Exception ignore) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path);
        }
    }

    /**
     * Indexes a single document
     */
    private static void indexDoc(IndexWriter writer, Path path)
            throws IOException, TikaException, SAXException {

        // make a new, empty document
        Document doc = new Document();
        File file = new File(path.toString());

        // Add the path of the file as a field named "path".  Use a
        // field that is indexed (i.e. searchable), but don't tokenize
        // the field into separate words and don't index term frequency
        // or positional information:
        Field pathField = new StringField("path", path.toString(), Field.Store.YES);
        doc.add(pathField);

        // Reading the contents of the file.
        ContentHandler handler = new BodyContentHandler();
        FileInputStream is = new FileInputStream(file);

        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, file.getCanonicalPath());

        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();
        parser.parse(is, handler, metadata, context);

        doc.add(new TextField("contents", handler.toString(), Field.Store.YES));


        System.out.println("added " + path);
        writer.addDocument(doc);

    }


}
