package eu.horako.stemmer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 * 
 * Usage:
 * Create an AffixStemmer object with Dictionary and AffixRuleSet and run 
 * {@see stem(String)} for each word you want to find stem(s) for.
 * 
 * 
 * Co uz to umi:
 * - zretezena pravidla
 * - sticky (reapply) pravidla - i na vice levelu; mozny problem s nekonecnym cyklem?
 * - invalid rules
 * - vice rulesetu pro jedno slovo(?)
 * - cross-product rules
 * - moznost vypnout kontrolu vuci slovniku (hodi se pro vytipovani pravidel u neznamych slov)
 * 
 * BUGS:
 * - s "NODIA" slovnikem + aff to dela problemy napr. se slovy "praze", "prasete" NENI PROBLEM ALGORITMU, ALE SLOVNIKU!
 * 
 * 
 * TODO:
 * - poresit cykly ve zretezeni (specialne pro sticky rules) VYSOKA PRIO, FIXED?
 * - podpora nekolika ruznych kombinaci pravidel pro jedno lemma 
 *   (1 slovo muze byt podle kontextu zpracovavano jinak - napr. "kout" - 
 *   extra pravidla pro sloveso a podst. jmeno) + podpora nacitani vice 
 *   sad pravidel ze slovniku; tohle uz myslim v hunspell stemmeru je; 
 *   VYSOKA PRIO, FIXED
 * - podpora "invalid" rules - pravidla pouzita pro rekurzivni zpracovani, 
 *   ktera ale sama negeneruji korektni tvar - treba pes => ps (a dale se 
 *   sklonuje standardne dle vzoru pan); tohle nema vyznam pro stemmer (proste
 *   staci dane pravidlo nepouzivat ve slovniku), ale je to potreba pro expander,
 *   aby to slovo nepridaval do platnych tvaru STREDNI PRIO, FIXED
 * - jak resit efektivne zmenu delky samohlasky v koreni (krava,trava); 
 *   STREDNI PRIORITA (mozna lze resit predchozim bodem?)
 * - sticky pravidla na vice nez 1 level (ma to smysl? jaka bude presna logika? 
 *   zvlast, jak to bude v kombinaci s crossProduct=true/false?); NIZKA PRIO, FIXED
 * - sticky pravidla jako jiny rule - zkratka nemusim vzdy pouzit stejne 
 *   pravidlo, muzu tam prilepit neco jineho; nizka prio, v cestine to asi neni 
 *   moc potreba, zatim si vystacime se stavajicim stavem NIZKA PRIO
 * - zabudovat podporu pro case-folding a ascii-folding (bud jen pro nacitani 
 *   .aff a .dic, nebo i pro vstupni slova - coz se ale v Solru da resit extra
 *   filtry; mozna to nejlepe udelat vsechno konfigurovatelne); pozn. tohle muze
 *   narusit kvalitu stemmingu - orezanim diakritiky muze matchovat vice pravidel
 *   nez s diakritikou, nutno vyresit VYSOKA PRIO
 * 
 * - zavest kvalitni logovani pres standardni logger nebo slf4j, log4j nebo tak neco, 
 *   zahodit System.err.print; STREDNI PRIO
 * - mozna nejaka optimalizace pri dohledavani slov ve slovniku - tvary, ktere 
 *   vznikaji, jsou casto shodne, ale generovane jinymi pravidly, treba 
 *   by se to dalo nejak zgrupovat a pak to overit najednou; mozna by stacilo 
 *   jen pri hledani drzet info o poslednim slovu hledanem ve slovniku; 
 *   pozn.: mohlo by tomu pomoci i usporadani affix pravidel v affix rule setu
 *   STREDNI PRIO
 * - DOKUMENTACE!!! Asi ne ke kodu, ten je stejne jen experimentalni,
 *    ale spis k formatu slovniku a affix souboru;STREDNI PRIO
 * - ??? moznost pojmenovat typy pravidel a umoznit jejich zapnuti/vypnuti??? 
 *   Napr. odvozeni prislovci od pridavnych jmen typu hezky / hezce STREDNI PRIO
 * - ??? Moznost oznacit slovni druhy ve slovniku
 * - Zatim jen k zamysleni - jak resit situace u stupnovani, kdy 1. a 3. 
 *   stupen lze povazovat za navzajem vyznamove blizsi nez 1. a 2. stupen 
 *   (protoze 1. a 3. stupen vyjadruji neco absolutniho, 2. stupen je relativni)
 *   Tohle by mozna slo resit pres invalid rule a sticky rule (jednou to tam mit
 *   jako invalid pro generovani 1. a 3. stupne, podruhe jako sticky, ale 
 *   bez zretezeni na 3. stupen) NIZKA PRIO
 * - (mozna nekdy) podpora jinych charsetu nez UTF-8; osobne bych se na to ale 
 *     nejspis vykaslal, zkonvertovat si ty slovniky a affix rules muze kazdy...
 *   NERESIT!
 * - nektere z tech vyse uvedenych veci si vyzadaji tridu, ktera bude vedle 
 *   kandidata na lemma ("possible stem") drzet i nejake dalsi informace, 
 *   hlavne pravidlo, kterym byl ten kandidat vygenerovan (nebo mozna kompletni 
 *   stack pravidel - aby se treba dal udelat ten kompletni reapply?) FIXED
 * - nejaka administrace? STREDNI PRIO
 * 
 */
public class AffixStemmer implements IAffixProcessor {
    private final Dictionary dictionary;
    private final AffixRuleSet ruleSet;
    private boolean checkAgainstDictionary = true;
    AffixRule emptyRule;

    
    /**
     * Create the stemmer with given dictionary and a rule set.
     * @param ruleSet expansion rules for the dictionary
     * @param dictionary dictionary
     */
    public AffixStemmer(AffixRuleSet ruleSet, Dictionary dictionary) {
        this.ruleSet = ruleSet;
        this.dictionary = dictionary;
        this.emptyRule = new AffixRule(AffixRuleType.SFX,"","","",null,".",true,null);
    }

    /**
     * Stem one word. This is the main method you should use to run the stemmer
     * on a word.
     * 
     * @param word the word to be stemmed
     * @return set of stems; when there are no stems found, the set will be empty; it is never null
     */
    public Set<String> stem(String word) {
        Set<String> stems = new HashSet<String>();
        Collection<WordRule> reduceSuffix = this.reduceAffix(word, AffixRuleType.SFX);
        Collection<WordRule> reducePrefix = this.reduceAffix(word, AffixRuleType.PFX);

        if(reduceSuffix != null) {
            for(WordRule wr : reduceSuffix) {
                if(wr.rule == null) {
                    if(this.dictContains(word)) {
                        stems.add(wr.word);
                    }
                    continue;
                }
                if(this.dictContains(wr.word, wr.rule.getFlag())) {
                    stems.add(this.expandStickyRule(wr));   
                }
            }
        }
        
        if(reducePrefix != null) {
            for(WordRule wr : reducePrefix) {
                if(wr.rule == null) {
                    if(this.dictContains(word)) {
                        stems.add(wr.word);
                    }
                    continue;
                }
                if(this.dictContains(wr.word, wr.rule.getFlag())) {
                    stems.add(this.expandStickyRule(wr));   
                }
            }
        }

        
        if(reduceSuffix!=null && reducePrefix!=null) {
            Collection<WordRulePair> reduceCombination = this.reduceCombined(reduceSuffix, reducePrefix);
            
            for(WordRulePair wrp : reduceCombination) {
                if(this.dictContains(wrp.word, wrp.pfxWordRule.rule.getFlag(), wrp.sfxWordRule.rule.getFlag())) {
                    stems.add(this.expandStickyRulePair(wrp));
                }
                else { // one should be an expansion rule of the other or vice versa...
                    AffixRule rsfx = wrp.sfxWordRule.rule;
                    AffixRule rpfx = wrp.pfxWordRule.rule;
                    if(this.dictContains(wrp.word, rsfx.getFlag()) &&
                            rsfx.hasExpansionFlag(rpfx.getFlag()) ||
                       this.dictContains(wrp.word, rpfx.getFlag()) &&
                            rpfx.hasExpansionFlag(rsfx.getFlag())) {
                        stems.add(this.expandStickyRulePair(wrp));
                    }
                }
            }
        }

        return stems;
    }

    private String expandStickyRule(WordRule wr) {
        String word = wr.word;
        while(wr.rule!=null && wr.rule.isSticky()) {
            word = wr.rule.apply(word);
            wr = wr.previous;
        }
        return word;
    }
    
    private String expandStickyRulePair(WordRulePair wrp) {
        WordRule wr = wrp.sfxWordRule;
        String word = wrp.word;
        while(wr!=null && wr.rule!=null && wr.rule.isSticky()) {
            word = wr.rule.apply(word);
            wr = wr.previous;
        }
        wr = wrp.pfxWordRule;
        while(wr!=null && wr.rule!=null && wr.rule.isSticky()) {
            word = wr.rule.apply(word);
            wr = wr.previous;
        }
        return word;
    }
    
    
    // Udelat to nasledovne:
    // - zredukovat SFX, co to pujde - kazda redukce generuje slovo+pravidlo
    // - zredukovat stejne PFX
    // - vzit vsechny zredukovane SFX, aplikovat na ne PFX redukci, co to pujde, preskocit dvojice, kde SFX nebo PFX pravidlo je non-cross
    // - vybrat slova, ktera jsou ve slovniku s prislusnym generujicim pravidlem/pravidly
    // - aplikovat vsechna sticky pravidla - pro ne si budeme muset pamatovat nejen generujici pravidla, ale cely retezec sticky pravidel

    // zkombinovat vysledky z reduceAffix (pfx a sfx) - zkusit vsechny kombinace - sice mozno pouzit reduceAffix pro 
    // opacny typ affixu, ale zkombinovat to rovnou tady bude znacne efektivnejsi
    // projdeme vsechny suffix redukce a pro kazdou z nich zkusime aplikovat vsechny prefix redukce, tj. MxN
    // platne kombinace jsou jen ty, ktere maji obe pravidla cross }obe musi byt pro dane slovo ve slovniku) 
    // nebo jedno pravidlo ma za reduction rule to druhe (to druhe musi byt ve slovniku)
    private Collection<WordRulePair> reduceCombined(Collection<WordRule> sfxReduction, Collection<WordRule> pfxReduction) {
        Collection<WordRulePair> combinedWordRules = new ArrayList<WordRulePair>();
        for(WordRule sfxRule: sfxReduction) {
            if(sfxRule.rule == null) { continue; }
            for(WordRule pfxRule: pfxReduction) {
                if(pfxRule.rule == null) { continue; }
                String combinedStem = this.combineRules(sfxRule, pfxRule);
                if(combinedStem == null) {
                    continue;
                }
                combinedWordRules.add(new WordRulePair(combinedStem, sfxRule,pfxRule));
            }
        }
        return combinedWordRules;
    }
    
    private String combineRules(WordRule sfxRule, WordRule pfxRule) {
        if(sfxRule.removedFromOrig + pfxRule.removedFromOrig > sfxRule.origLength) {
            return null;
        }
        return pfxRule.word.substring(0, pfxRule.addedToOrig) + 
                sfxRule.word.substring(pfxRule.removedFromOrig);
    }
    
    /**
     * Finds all affix reductions of one affix type (suffix, prefix)
     * The reductions may not be valid words, that must be further verified by the dictionary
     * @param word
     * @param type
     * @return
     */
    protected Collection<WordRule> reduceAffix(String word,AffixRuleType type) {
        Collection<WordRule> allWordRules = new ArrayList<WordRule>();
        WordRule topRule = new WordRule(word,null,null);
        allWordRules.add(topRule);
        Collection<WordRule> wordRules = this.reduceAffixFirstStep(word,type,topRule);
        if(wordRules == null) {
            return allWordRules;
        }

        
        Collection<WordRule> nextWordRules = new ArrayList<WordRule>();

        allWordRules.addAll(wordRules);
        nextWordRules.addAll(wordRules);
        while(true) {
            wordRules.clear();
            wordRules.addAll(nextWordRules);
            nextWordRules.clear();
            for(WordRule wr: wordRules) {
                Collection<WordRule> newWordRules = this.reduceAffixNextStep(wr.word, wr, wr.rule.getReductionRules().getAll(),type);
                if(newWordRules == null) {
                    continue;
                }
                allWordRules.addAll(newWordRules);
                nextWordRules.addAll(newWordRules);
            }
            if(nextWordRules.isEmpty()) {
                break;
            }
        }
        
        return allWordRules;
    }
    
    
    protected Collection<WordRule> reduceAffixFirstStep(String word, AffixRuleType type, WordRule topRule) {
        List<WordRule> newRules = null;
        for(int i=0 ; i <= word.length(); i++) {
            String affix;
            Collection<AffixRule> rules;
            if(type == AffixRuleType.PFX) {
                affix = word.substring(0,i);
                rules = this.ruleSet.pfxRulesByAffix.get(affix);
            }
            else if(type == AffixRuleType.SFX) {
                affix = word.substring(word.length()-i,word.length());
                rules = this.ruleSet.sfxRulesByAffix.get(affix);
            } else {
                continue;
            }
            for(AffixRule r: rules) {
                if(r.getType() != type) { continue; } // TODO log warning - this should never happen!
                if(r.isInvalid()) { continue; }
                String stem = r.stemWord(word);
                if(stem == null) { continue; }
                WordRule affixWordRule = new WordRule(stem,r,topRule);
                if(newRules ==  null) {
                    newRules = new ArrayList<WordRule>();
                }
                newRules.add(affixWordRule);
            }
        }
        return newRules;
    }

    private Collection<WordRule> reduceAffixNextStep(String word, WordRule previousWR, Collection<AffixRule> possibleRules, AffixRuleType type) {
        if(possibleRules == null) { return null; } 
        List<WordRule> newRules = null;
        
        for(AffixRule r: possibleRules) {
            if(r.getType() != type) { continue; }
            String stem = r.stemWord(word);

            if(stem == null) { continue; }
            WordRule afxWordRule = new WordRule(stem,r,previousWR);
            if(newRules ==  null) {
                newRules = new ArrayList<WordRule>();
            }
            newRules.add(afxWordRule); 
        }

        return newRules;
    }


    
    

    @Override
    public Set<String> process(String word) {
        return this.stem(word);
    }

    public boolean isCheckAgainstDictionary() {
        return checkAgainstDictionary;
    }

    /**
     * This turns on/off checking stem candidates against the dictionary.
     * Turning the checking off means that all stem candidates will be considered
     * valid stems.
     * 
     * @param checkAgainstDictionary When set to true, the possible stems found are checked against the dictionary,
     * otherwise they are considered to be valid stems; default value is true.
     */
    public void setCheckAgainstDictionary(boolean checkAgainstDictionary) {
        this.checkAgainstDictionary = checkAgainstDictionary;
    }

    private boolean dictContains(String word) {
        if(this.checkAgainstDictionary) return this.dictionary.contains(word);
        else return true;
    }
    
    private boolean dictContains(String word,String affix) {
        if(this.checkAgainstDictionary) return this.dictionary.contains(word,affix);
        else return true;
    }
    
    private boolean dictContains(String word,String pfx,String sfx) {
        if(this.checkAgainstDictionary) return this.dictionary.contains(word,pfx,sfx);
        else return true;
    }
    

    public class ReductionRules {
        String word;
        AffixRule sfxRule, pfxRule; // rules leading to this word
        Set<AffixRule> redRules; // reduction rules to follow
        
        public ReductionRules(String word, AffixRule sfxRule, AffixRule pfxRule, Set<AffixRule> reductionRules) {
            this.word = word;
            this.redRules = new HashSet<AffixRule>(reductionRules);
            this.sfxRule = sfxRule;
            this.pfxRule = pfxRule;
        }
    }
    
    public class WordRule {
        String word;
        AffixRule rule = null;
        WordRule previous = null;
        int removedFromOrig;
        int addedToOrig;
        int origLength;
        
        public WordRule(String word, AffixRule rule, WordRule previous) {
            this.word = word;
            this.rule = rule;
            this.previous = previous;
            if(previous != null) {
                this.origLength = this.previous.origLength;
                this.removedFromOrig = Math.max(previous.removedFromOrig,previous.origLength - this.word.length() + this.rule.getRemove().length());
                this.addedToOrig = this.word.length() - this.origLength + this.removedFromOrig;
            } else {
                this.removedFromOrig = 0;
                this.addedToOrig = 0;
                this.origLength = this.word.length();
            }
        }
        
    }

    public class WordRulePair {
        String word;
        WordRule pfxWordRule;
        WordRule sfxWordRule;
        
        public WordRulePair(String word, WordRule sfxWordRule, WordRule pfxWordRule) {
            this.word = word;
            this.sfxWordRule = sfxWordRule;
            this.pfxWordRule = pfxWordRule;
        }
    }


}
