package eu.horako.stemmer;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class AffixRule {
    private final AffixRuleType type;
    private final String remove;
    private final String append;
    private int appendLength;
    private final String flag; 
    private Set<String> expansionFlags = null; // 
    private final AffixMap<String,AffixRule> expansionRules; // additional rules - key is the append affix of the additional rules, value is list of the rules with that affix
    private final AffixMap<String,AffixRule> expansionRulesNoLoop; // same as previous, but omits rules with the same flag as this
    private final AffixMap<String,AffixRule> reductionRules; // reversed list of additional rules - contains the rules which have this rule as additional (i.e. if rules that have this rule in expansionRules); key is the append affix of the other rules
    private final boolean crossProduct; // whether this rule can be combined with other type of rules (SFX combined with PFX and vice versa)
    private boolean sticky; // when true, reapply after stemming (typically negation prefix); de facto says that this rule generates a new word (not just a word form)
    private final Pattern condition; // regex condition - whether this rule can be applied to a word
    private boolean invalid; // rule generating invalid word form (can be used as an intermediate word form for another rules)
    private final String strCondition;

    
    public AffixRule(AffixRuleType type, String flag, String remove, String append,
            Set<String> addFlags, String condition, boolean crossProduct, String[] properties) {
        this.type = type;
        this.flag = flag;
        this.remove = remove;
        this.append = append;
        this.condition = this.type==AffixRuleType.SFX?Pattern.compile(condition+"$"):Pattern.compile("^"+condition);
        this.strCondition = condition;
        this.crossProduct = crossProduct;
        this.expansionFlags = addFlags;
        this.expansionRules = new AffixMap<String,AffixRule>(); // jen rules stejneho typu (PFX/SFX), jako je tenhle - vazba podle expansionFlags
        this.expansionRulesNoLoop = new AffixMap<String,AffixRule>(); // jen rules stejneho typu (PFX/SFX), jako je tenhle - vazba podle expansionFlags
        this.reductionRules = new AffixMap<String,AffixRule>(); // jen rules stejneho typu (PFX/SFX), jako je tenhle - vazba podle expansionFlags
        this.sticky = false;
        this.invalid = false;

        if(properties != null) {
            for(String p : properties) {
                p = p.trim();
                if(p.trim().equalsIgnoreCase("sticky")) { this.sticky = true; }
                else if(p.trim().equalsIgnoreCase("invalid")) { this.invalid = true; }
            }
        }
    }
    
    public String stemWord(String word) {
        if(this.append.length() > word.length()) { return null; }
        String possibleStem;
        if(this.getType() == AffixRuleType.SFX) {
            if(!word.endsWith(this.append)) { return null; }
            if(this.append.isEmpty()) { possibleStem = word + this.remove; }
            else { possibleStem = word.substring(0, word.length() - this.append.length()) + this.remove; }
        }
        else if(this.getType() == AffixRuleType.PFX) {
            if(!word.startsWith(this.append)) { return null; }
            possibleStem = this.remove + word.substring(this.append.length());
        }
        else { // this shouldn't happen...
            return null; 
        }

        if(this.getCondition().matcher(possibleStem).find()) {
            return possibleStem;
        }
        else {
            return null;
        }
    }
    

    /**
     *  
     * @param word
     * @return false if the rule cannot be applied to the word; new (derived) word if the rule can be applied
     */        
    public String apply(String word) {
        if(this.remove.length() > word.length()) { return null; }
        if(!this.condition.matcher(word).find()) { return null; }

        if(this.type == AffixRuleType.SFX) {
            if(this.remove.isEmpty()) { return word + this.append; }
            else { return word.substring(0,word.length() - this.remove.length()) + this.append; }
        }
        else if(this.type == AffixRuleType.PFX) {
            return this.append + word.substring(this.remove.length());
        }
        else {
            return null;
        }

    }
    
    
    // TODO udelat to rekurzivne podle expansionRules (pokud jsou sticky), vracet Set<String>; rekurze vyzaduje nastaveni limitu nebo loop-check!
    public String possiblyReapply(String word) {
//System.err.println("Reapply: sticky = " + this.sticky + ", word=" + word + ", append=" + this.append + ", remove=" + this.remove);
        if(!this.sticky) { return word; }
        String nw = this.apply(word);
        if(nw != null) { return nw; }
        else { return word; }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("TYPE:").append(this.type).
          append(" FLAG:").append(this.flag).
          append(" REMOVE:").append(this.remove).
          append(" APPEND:").append(this.append).
          append(" CONDITION:").append(this.condition).
          append(" CROSS-PRODUCT:").append(this.crossProduct).
          append(" INVALID:").append(this.invalid);

        if(!this.reductionRules.isEmpty()) {
            s.append(" ADD_RULES_REVERSED:");
/* TODO
                $a=array();
                foreach($this->addRulesReversed as $rr) {
                    foreach($rr as $r) $a[$r->flag]=1;
                }
                $s .= implode(",",array_keys($a));
*/
        }
        if(!this.expansionRules.isEmpty()) {
            s.append(" ADD_RULES_STRAIGHT:");
/* TODO
            $a=array();
                foreach($this->addRulesStraight as $rr) {
                    foreach($rr as $r) $a[$r->flag]=1;
                }
                $s .= implode(",",  array_keys($a));
*/
        }
        return s.toString();
    }

    
    
    public String getRemove() {
        return remove;
    }

    public String getAppend() {
        return append;
    }

    public String getFlag() {
        return flag;
    }

    public AffixRuleType getType() {
        return type;
    }

    public int getAppendLength() {
        return appendLength;
    }

    public Set<String> getExpansionFlags() {
        return expansionFlags;
    }

    public AffixMap<String,AffixRule> getExpansionRules() {
        return this.expansionRules;
    }

    public AffixMap<String,AffixRule> getExpansionRulesNoLoop() {
        return this.expansionRulesNoLoop;
    }

    public AffixMap<String,AffixRule> getReductionRules() {
        return this.reductionRules;
    }

    public boolean isCrossProduct() {
        return crossProduct;
    }

    public boolean isInvalid() {
        return this.invalid;
    }

    public boolean isSticky() {
        return this.sticky;
    }

    public Pattern getCondition() {
        return condition;
    }

    public String getStrCondition() {
        return strCondition;
    }
    
    public boolean hasExpansionFlag(String f) {
        return this.expansionFlags!=null && this.expansionFlags.contains(f);
    }

    
    public void setAddRuleStraight(AffixRule rule) {
        this.expansionRules.add(rule.getAppend(), rule);
        if(!rule.getFlag().equals(this.getFlag())) { 
            this.expansionRulesNoLoop.add(
                     rule.getAppend(), 
                     rule);
        }
    }
    
    public void setAddRulesStraight(Collection<AffixRule> rules) {
        for(AffixRule rule : rules) {
            this.setAddRuleStraight(rule);
        }
    }

    public void setAddRuleReverse(AffixRule rule) {
        this.reductionRules.add(rule.getAppend(), rule);
    }
    
    public void setAddRulesReverse(Collection<AffixRule> rules) {
        for(AffixRule rule : rules) {
            this.reductionRules.add(rule.getAppend(), rule);
        }
    }

    public boolean canMatchSameWord(AffixRule otherRule) {
        if(otherRule.getType() != this.getType()) return true;
        
        return false;
    }
    
    
}
