package eu.horako.stemmer.lucene;

import eu.horako.stemmer.AffixStemmer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public final class StemFilter extends TokenFilter {
    private final PositionIncrementAttribute posIncAtt = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
    private final AffixStemmer stemmer;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private State savedState;
    List<String> buffer;

    public StemFilter(TokenStream input, AffixStemmer stemmer) {
        super(input);
        this.stemmer = stemmer;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (buffer != null && !buffer.isEmpty()) { // more stems from previous run
            String nextStem = buffer.remove(0); // get stem
            restoreState(savedState); // restore previous state of the token stream
            posIncAtt.setPositionIncrement(0); // this otput token has the same position in the text as previous

            char[] stemBuffer = new char[nextStem.length()];
            nextStem.getChars(0, nextStem.length(), stemBuffer, 0); //!!!
            termAtt.copyBuffer(stemBuffer, 0, stemBuffer.length);
            return true;
        }

        if (!input.incrementToken()) {
            return false;
        }

        buffer = new ArrayList<>();
        buffer.addAll(stemmer.stem(termAtt.toString()));

        String stem;
        if(buffer.isEmpty()) { // we do not know this word, return it unchanged;
            return true;
        } else {
            stem = buffer.remove(0);
        }

        termAtt.copyBuffer(stem.toCharArray(), 0, stem.length());
        if(!buffer.isEmpty()) { // save state to be restored in the next round
            savedState = captureState();
        }

        return true;
    }

}
