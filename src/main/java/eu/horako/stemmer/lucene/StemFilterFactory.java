package eu.horako.stemmer.lucene;

import eu.horako.stemmer.AffixFormatException;
import eu.horako.stemmer.AffixRuleSet;
import eu.horako.stemmer.AffixStemmer;
import eu.horako.stemmer.Dictionary;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 * 
 * Factory for {@see StemFilter}. 
 * <pre class="prettyprint" > 
 * &lt;fieldType name="text_czstem" class="solr.TextField" positionIncrementGap="100"&gt;
 * &lt;analyzer&gt; &lt;tokenizer class="solr.StandardTokenizerFactory"/&gt;
 * &lt;filter class="solr.LowerCaseFilterFactory"/&gt; &lt;filter
 * class="solr.StemFilterFactory"/&gt; &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
public class StemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    private AffixStemmer stemmer;
    private String dictFile = null;
    private String affixRulesFile = null;
    private ResourceLoader loader;
    private boolean lowerCase = false;

    /**
     *
     * @param args
     */
    public StemFilterFactory(Map<String,String> args) {
        super(args);
        dictFile = args.get("dictionary").trim();
        affixRulesFile = args.get("affix");
        String lowerCaseStr = args.get("lowerCase");
        lowerCase = lowerCaseStr != null && !lowerCaseStr.isEmpty() && (lowerCaseStr.charAt(0)=='1' || lowerCaseStr.toLowerCase().charAt(0)=='t');
    }
    
    @Override
    public void inform(ResourceLoader loader) {
         this.loader = loader;
    }
    
    @Override
    public TokenStream create(TokenStream input) {
        try {
            InputStream rulesStream = this.loader.openResource(affixRulesFile);
            AffixRuleSet rules = new AffixRuleSet(rulesStream, lowerCase);
            InputStream dictStream = this.loader.openResource(dictFile);
            Dictionary dict = new Dictionary(dictStream, rules, lowerCase);
            this.stemmer = new AffixStemmer(rules,dict);
        } catch (AffixFormatException | IOException ex) {
            throw new RuntimeException("Unable to load LMCStemFilter data! [dictionary=" + dictFile + ",affix=" + affixRulesFile + "]", ex);
        }

        return new StemFilter(input,this.stemmer);
    }
}
