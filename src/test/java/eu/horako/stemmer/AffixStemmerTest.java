/**
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
package eu.horako.stemmer;

import eu.horako.stemmer.AffixStemmer.WordRule;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Ondra
 */
public class AffixStemmerTest {
    AffixStemmer stemmer;
    
    String dictInput = 
       "3\n" +
       "pán/P1\n" +
       "pes/XXAA\n" +
       "bůh/XXAABBCCDD\n" +
       "vůl/XX\n" + 
       "praha/P5";
    
    String affixInput = 
        "FLAG long\n" +
        "SET UTF-8\n" +
        "TRY aáeéěiíoóuúůyýbcčdďlmnňfjkprřsštťvghqwxzžAÁEÉĚIÍOÓUÚŮYÝBCČDĎLMNŇFJKPRŘSŠTŤVGHQWXZŽ\n" +
        "\n" +
        "SFX XX Y 5\n" +
        "SFX XX   es          s/P1         pes  invalid\n" +
        "SFX XX   ůl          ol/P1        vůl  invalid\n" +
        "SFX XX   ůh          oh/P1        bůh  invalid\n" +
        "SFX XX   h           žek/P2       bůh  sticky\n" +
        "SFX XX   ý           ější/P3P4    ný  sticky\n" +
        "\n" +

        "PFX YY Y 1\n" +
        "PFX YY   0           ne         .  reapply,sticky\n" +
        "\n" +
        "PFX ZZ Y 1\n" +
        "PFX ZZ   0           ne         .  reapply,sticky\n" +
        "\n" +
        "PFX AA Y 1\n" +
        "PFX AA   0           praso      .  reapply,sticky\n" +
        "\n" +
        "PFX BB Y 1\n" +
        "PFX BB   0           polo/YY      .  reapply,sticky\n" +
        "\n" +
        "PFX CC Y 1\n" +
        "PFX CC   0           skoro/YY      .  reapply,sticky\n" +
        "\n" +
        "PFX DD Y 1\n" +
        "PFX DD   0           lži/ZZ      .  reapply,sticky\n" +
        "\n" +
        "SFX P1 Y 5\n" +
        "SFX P1   0           a          [^aeok]\n" +
        "SFX P1   0           u          [^aeoku]\n" +
        "SFX P1   0           ovi        [^aeok]\n" +
        "SFX P1   0           e          [^aeokurcgh]\n" +
        "SFX P1   r           ře         [^aeiouyáéíóúůýě]r\n" +
        "\n" +
        "SFX P2 Y 3\n" +
        "SFX P2   ek          ka         ek\n" +
        "SFX P2   ek          ku         ek\n" +
        "SFX P2   ek          kovi       ek\n" +
        "\n" +
        "PFX P3 Y 1\n" +
        "PFX P3   0           nej        .\n" +
        "\n" +
        "SFX P4 Y 2\n" +
        "SFX P4   0          ho         í\n" +
        "SFX P4   0          mu         í\n" + 
        "\n" +
        "SFX P5 Y 1\n" +
        "SFX P5   ha         ze         [^c]ha\n";
    
    public AffixStemmerTest() {
        
    }
    
    
    @Test
    public void stemmerTestChainedRule() {
        Set<String> words = this.stemmer.stem("psa");
        String[] expected = {"pes"};
        
        Assert.assertEquals(expected.length, words.size());
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }

    }

    @Test
    public void stemmerTestChainedStickyRule() {
        Set<String> words = this.stemmer.stem("polobůžka");
        String[] expected = {"polobůžek"};
        
        Assert.assertEquals(expected.length, words.size());
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }

    }
    
    
    @Test
    public void stemmerTestCombinedRule1() { // 
        Set<String> words = this.stemmer.stem("prasopsa");
        String[] expected = {"prasopes"};
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
        Assert.assertEquals(expected.length, words.size());

    }

    @Test
    public void stemmerTestCombinedRule2() { // combined chained stemming and chained sticky rules application of both prefix and suffix
        Set<String> words = this.stemmer.stem("nepolobůžkovi"); // will be reduced to "bůh" and then expanded again by sticky rules
        String[] expected = {"nepolobůžek"};
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
        Assert.assertEquals(expected.length, words.size());
    }
    
    
    @Test
    public void stemmerTestReduceSuffix() { // suffix reduction (doesn't necessarily generate valid words)
        Collection<WordRule> words = this.stemmer.reduceAffix("psa", AffixRuleType.SFX);
        String[] expected = {"pes","psa","ps"};
        Set<String> wordSet = new HashSet<String>();
        for(WordRule wr : words) {
            boolean found = false;
            for(String w : expected) {
                if(w.equals(wr.word)) {
                    found = true;
                    wordSet.add(w);
                    break;
                }
            }
            Assert.assertTrue(found);
        }
        Assert.assertEquals(expected.length,wordSet.size());
    }

    
    @Test
    public void stemmerTestReducePrefix() { // prefix reduction (doesn't necessarily generate valid words)
        Collection<WordRule> words = this.stemmer.reduceAffix("prasopsa", AffixRuleType.PFX);
        String[] expected = {"prasopsa","psa"};
        Set<String> wordSet = new HashSet<String>();
        for(WordRule wr : words) {
            boolean found = false;
            for(String w : expected) {
                if(w.equals(wr.word)) {
                    found = true;
                    wordSet.add(w);
                    break;
                }
            }
            Assert.assertTrue(found);
        }
        Assert.assertEquals(expected.length,wordSet.size());
    }
   
 
    @Test
    public void stemmerTestSimple() { // just simple one-suffix stemming
        Set<String> words = this.stemmer.stem("pánovi");
        String[] expected = {"pán"};
        Assert.assertEquals(expected.length, words.size());
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
    }


    
    
    @Test
    public void stemmerTestInvalid1() { // non-existent word that can be generated by some "invalid" rule
        Set<String> words = this.stemmer.stem("nepoloboh");
        String[] expected = {};
        
        Assert.assertEquals(expected.length, words.size());
    }

    @Test
    public void stemmerTestInvalid2() { // non-existent word that can be generated by some "invalid" rule
        Set<String> words = this.stemmer.stem("boh");
        String[] expected = {};
        
        Assert.assertEquals(expected.length, words.size());
    }
    
    @Test
    public void stemmerTestAlreadyStemmed1() { // a word that doesn't need to be stemmed as it's already a stem
        Set<String> words = this.stemmer.stem("bůh");
        String[] expected = {"bůh"};
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }

    @Test
    public void stemmerTestAlreadyStemmed2() { // sticky rule applied on the prefix side
        Set<String> words = this.stemmer.stem("polobůh");
        String[] expected = {"polobůh"};
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }
    
    @Test
    public void stemmerTestAlreadyStemmed3() {  // sticky rule applied on the suffix side
        Set<String> words = this.stemmer.stem("bůžek");
        String[] expected = {"bůžek"};
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }

    
    
    @Test
    public void stemmerTestUnknownWord() { // the word is unknown, i.e. not in dictionary nor generated by the rules
        Set<String> words = this.stemmer.stem("pivo");
        Assert.assertEquals(0, words.size());
    }

    @Test
    public void stemmerSimpleTest1() {  // sticky rule applied on the suffix side
        Set<String> words = this.stemmer.stem("praze");
        String[] expected = {"praha"};
        for (String expectedWord : expected) {
            Assert.assertTrue("Stemmed words must contain " + expectedWord, words.contains(expectedWord));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }

    
    
    @Before
    public void setUp() throws IOException, AffixFormatException {
        Reader dictReader = new StringReader(this.dictInput);
        Reader affixReader = new StringReader(this.affixInput);
        AffixRuleSet ruleSet = new AffixRuleSet(affixReader);
        Dictionary dict = new Dictionary(dictReader,ruleSet);
        this.stemmer = new AffixStemmer(ruleSet,dict);
    }
    
    @After
    public void tearDown() {
    }
}
