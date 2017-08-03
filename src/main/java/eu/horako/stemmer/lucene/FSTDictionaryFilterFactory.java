package eu.horako.stemmer.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.solr.common.SolrException;

/**
 * Replaces terms according to a dictionary stored in a FST file.
 * 
 * Example filter config:
 * &lt;filter class="eu.horako.stemmer.lucene.FSTDictionaryFilterFactory" fst="cfg/cs.fst" separator="|" /&gt;
 *
 * fst - FST dictionary file (on the filesystem, not in the collection's config)
 * separator - values separator in the FST file
 * 
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class FSTDictionaryFilterFactory extends TokenFilterFactory {
    private String fstFile = null;
    private String separator = null;

    public FSTDictionaryFilterFactory(Map<String,String> args) {
        super(args);
        fstFile = args.get("fstFile");
        separator = args.get("separator");
    }

    
    @Override
    public TokenStream create(TokenStream input) {
        try {
            FST<CharsRef> fst = loadFST(fstFile);
            return new FSTDictionaryFilter(input, fst, separator);
        } catch (Exception ex) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to load FSTDictionaryFilter data! [fstFile=" + fstFile + "]", ex);
        }
    }

    private FST<CharsRef> loadFST(String fname) throws IOException {
        CharSequenceOutputs outputs = CharSequenceOutputs.getSingleton();
        FST<CharsRef> fst = (FST<CharsRef>)FST.read(new File(fname).toPath(), outputs);
        return fst;
    }
}
