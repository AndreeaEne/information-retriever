# information-retriever

Information retrieval for Romanian documents.

## Features:
- Diacritics: write with/without them and still get results
- Steamming: if you search for the word *Mama*, you cand find *Mamele, memelor, mamei*
- Stopwords: ignore words such as *şi*, *în*, *a*, *cu*, etc.
- Indexing and searching in any text files: .txt, .word, .rtf, .pdf, .html.
- Highlighter: find text snippets from a hit document, and highlight tokens matching the query.
- Limit search using: *last modified date* and *file format*. 


## Technology & Libraries
- Java 1.8.0_73
- [Apache Lucene 6.4.2](https://lucene.apache.org)
- [Apache Tika 1.14](https://tika.apache.org)


## Setup
1. Run *main.IndexFiles* for reading the files under the *Docs/* folder.
2. Run main.SearchFiles to look for the chosen words.
