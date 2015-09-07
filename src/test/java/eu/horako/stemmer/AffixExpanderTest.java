/**
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
package eu.horako.stemmer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;
import org.junit.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ondra
 */
public class AffixExpanderTest {
    AffixExpander expander;
    
    String dictInput = 
       "6\n"    +
       "tvrdý/Q1\n" +
       "pán/P1\n" +
       "pes/XXAA\n" +
       "bůh/XXAABB\n" +
       "člověk/P1CC\n"  +
       "kmán/P1\n"  +
       "kmán/P2\n"  +
       "vůl/XX\n";
    
    String affixInput = 
        "FLAG long\n" +
        "SET UTF-8\n" +
        "TRY aáeéěiíoóuúůyýbcčdďlmnňfjkprřsštťvghqwxzžAÁEÉĚIÍOÓUÚŮYÝBCČDĎLMNŇFJKPRŘSŠTŤVGHQWXZŽ\n" +
        "\n" +
        "SFX XX Y 3\n" +
        "SFX XX   es          s/P1         pes  invalid\n" +
        "SFX XX   ůl          ol/P1        vůl  invalid\n" +
        "SFX XX   ůh          oh/P1        bůh  invalid\n" +
        "\n" +
        "PFX YY Y 1\n" +
        "PFX YY   0           ne        .  reapply\n" +
        "\n" +
        "PFX AA Y 1\n" +
        "PFX AA   0           maxi      .  reapply\n" +
        "\n" +
        "PFX BB Y 1\n" +
        "PFX BB   0           polo/YY   .  reapply\n" +
        "\n" +
        "PFX CC N 1\n" +
        "PFX CC   0           opo/ZZ    .  reapply\n" +
        "\n" +
        "PFX ZZ Y 1\n" +
        "PFX ZZ   0           ne        .  reapply\n" +
        "\n" +
        "SFX P1 Y 5\n" +
        "SFX P1   0           a         [^aeo]\n" +
        "SFX P1   0           u         [^aeou]\n" +
        "SFX P1   0           ovi       [^aeo]\n" +
        "SFX P1   0           e         [^aeokurcgh]\n" +
        "SFX P1   r           ře        [^aeiouyáéíóúůýě]r\n" + 
        "\n" +
        "SFX P2 Y 1\n" +
        "SFX P2   0           i         .\n" +
        "SFX Q1 Y 1\n" +
        "SFX Q1   ý           ší/Q2         ý\n" +
        "\n" +
        "PFX Q2 Y 1\n" +
        "PFX Q2   0           nej           .\n" +
        "\n";
    
    public AffixExpanderTest() {
        
    }
    
    @Test
    public void expanderTestCombinedSuffixPrefix() {
        Set<String> words = this.expander.expand("pes", 20);
        String[] expected = {"pes","psa","psu","psovi","pse","maxipes","maxipsa","maxipsu","maxipsovi","maxipse"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }

        Assert.assertEquals(expected.length, words.size());
    }


    @Test
    public void expanderTestRecursionLimit() {
        Set<String> words = this.expander.expand("pes", 1);
        String[] expected = {"pes","maxipes"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }
        Assert.assertEquals(expected.length, words.size());

        words = this.expander.expand("pes", 0);
        Assert.assertTrue(words.contains("pes"));
        Assert.assertEquals(1, words.size());
    }

    
    @Test
    public void expanderTestNoCross() {
        Set<String> words = this.expander.expand("člověk", 2);
        String[] expected = {"člověk","člověka","člověku","člověkovi",
                 "opočlověk","neopočlověk"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }

        Assert.assertEquals(expected.length, words.size());
    }
    
    
    
    @Test
    public void expanderTestSimple() {
        Set<String> words = this.expander.expand("pán", 20);
        String[] expected = {"pán","pána","pánu","pánovi","páne"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }

        Assert.assertEquals(expected.length, words.size());
    }

    @Test
    public void expanderTestComplex() {
        Set<String> words = this.expander.expand("bůh", 2);
        String[] expected = {"bůh","boha","bohu","bohovi","polobůh","poloboha","polobohu","polobohovi",
             "nepolobůh","nepoloboha","nepolobohu","nepolobohovi","maxibůh","maxiboha","maxibohu","maxibohovi"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }

    @Test
    public void expanderTestSuffixPrefixDependence() {
        Set<String> words = this.expander.expand("tvrdý", 2);
        String[] expected = {"tvrdý","tvrdší","nejtvrdší"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }
    
    @Test
    public void expanderTestMultipleRuleSet() {
        Set<String> words = this.expander.expand("kmán", 2);
        String[] expected = {"kmán","kmána","kmánu","kmánovi","kmáne","kmáni"};
        
        for(int i=0; i<expected.length; i++) {
            Assert.assertTrue("Expanded words must contain " + expected[i],words.contains(expected[i]));
        }
        
        Assert.assertEquals(expected.length, words.size());
    }
    
    
    @Test
    public void expanderTestUnknownWord() {
        Set<String> words = this.expander.expand("pivo", 20);
        Assert.assertEquals(0, words.size());
    }

    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws IOException, AffixFormatException {
        Reader dictReader = new StringReader(this.dictInput);
        Reader affixReader = new StringReader(this.affixInput);
        AffixRuleSet ruleSet = new AffixRuleSet(affixReader,false);
        Dictionary dict = new Dictionary(dictReader,ruleSet,false);
        this.expander = new AffixExpander(ruleSet,dict);
    }
    
    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
