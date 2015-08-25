package eu.horako.stemmer;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 * @param <F> first member of the pair
 * @param <S> second member of the pair
 */
public class Pair<F,S> {
    public F first;
    public S second;
    
    public Pair(F f,S s) {
        this.first = f;
        this.second = s;
    }
}
