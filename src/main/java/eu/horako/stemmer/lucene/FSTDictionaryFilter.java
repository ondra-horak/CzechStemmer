package eu.horako.stemmer.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public final class FSTDictionaryFilter extends TokenFilter {
    public static Logger logger = LoggerFactory.getLogger(FSTDictionaryFilter.class);
    private final PositionIncrementAttribute posIncAtt = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
    private final FST<CharsRef> fst;
    private final String separator;
  
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private State savedState;
    List<String> buffer;

    public FSTDictionaryFilter(TokenStream input, FST<CharsRef> fst, String separator) {
        super(input);
        this.fst = fst;
        this.separator = separator;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (buffer != null && !buffer.isEmpty()) { // more strings from previous run (more than 1 value for the key)
            String nextStem = buffer.remove(0); // get string from buffer
            restoreState(savedState); // restore previous state of the token stream
            posIncAtt.setPositionIncrement(0); // this output token has the same position in the text as previous

            char[] stemBuffer = new char[nextStem.length()];
            nextStem.getChars(0, nextStem.length(), stemBuffer, 0);
            termAtt.copyBuffer(stemBuffer, 0, stemBuffer.length);
            return true;
        }

        if (!input.incrementToken()) {
            return false;
        }

        buffer = new ArrayList<String>();
        buffer.addAll(getValuesFromFST(termAtt.toString()));

        String value;
        if(buffer.isEmpty()) { // we do not know this word, return it unchanged;
            return true;
        } else {
            value = buffer.remove(0);
        }

        termAtt.copyBuffer(value.toCharArray(), 0, value.length());
        if(!buffer.isEmpty()) { // save state to be restored in the next round
            savedState = captureState();
        }

        return true;
    }

    private List<String> getValuesFromFST(String key) {
        try {
            CharsRef charsRef = Util.get(fst, new BytesRef(key));
            if(charsRef == null) {
                return new ArrayList<>();
            }
            return Arrays.asList(charsRef.toString().split(separator));
        } catch (IOException ex) {
            logger.warn("Cannot get string from FST (key='" + key + "'", ex);
            return new ArrayList<>();
        }
        
    }

}
