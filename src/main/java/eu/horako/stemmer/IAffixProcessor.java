package eu.horako.stemmer;

import java.util.Set;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public interface IAffixProcessor {
    public Set<String> process(String word);
}
