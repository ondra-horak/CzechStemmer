package eu.horako.stemmer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Affix rules to expand words from the dictionary to other forms.
 * 
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 * 
 */
public class AffixRuleSet {
    FlagType flagType;
    AffixMap<String,AffixRule> rulesByFlag = new AffixMap<String,AffixRule>();
    AffixMap<String,AffixRule> pfxRulesByAffix = new AffixMap<String,AffixRule>(); // by append affix
    AffixMap<String,AffixRule> sfxRulesByAffix = new AffixMap<String,AffixRule>();  // by append affix
    
    private class AffixRuleBlock {
        private int count;
        private AffixRuleType type;
        private String flag;
        private boolean crossProduct;
    }
    
    public AffixRuleSet(String filename) throws IOException, AffixFormatException {
        InputStream input = new BufferedInputStream(new FileInputStream(new File(filename)));
        this.load(new InputStreamReader(input,"UTF-8"));
    }
    
    public AffixRuleSet(InputStream input) throws IOException, AffixFormatException {
        this.load(new InputStreamReader(input,"UTF-8"));
    }

    public AffixRuleSet(Reader reader) throws IOException, AffixFormatException {
        this.load(reader);
    }
    
    
    private void load(Reader r) throws IOException, AffixFormatException {
        BufferedReader reader = new BufferedReader(r);

        String[] params;
        String state = "OUT";
        this.flagType = FlagType.ASCII;
        AffixRuleBlock affRuleBlock = null;
        while(true) {
            String line = reader.readLine();
            if(line == null) { break; }
            params = line.trim().split("\\s+",6);
            
            if(params.length < 2 || params[0].charAt(0) == '#') { continue; }
            if(params[0].equals("FLAG")) {
                this.flagType = this.parseFlagLine(params);
            }
            else if(params[0].equals("PFX") || params[0].equals("SFX")) {
                if(state.equals("OUT")) {
                    affRuleBlock = this.parseAffixRuleBlock(params);
                    state = params[0];
                }
                else {
                    AffixRule rule = this.parseAffixRuleLine(params, affRuleBlock);
                    this.insertAffixRule(rule);
                    affRuleBlock.count--;
                    if(affRuleBlock.count <= 0) { state = "OUT"; continue;  }
                }
            }
        }
        this.buildRecursiveRules();
    }
    

    private void insertAffixRule(AffixRule r) {
        AffixMap<String,AffixRule> affMap;
        this.rulesByFlag.add(r.getFlag(), r);

        if(r.getType() == AffixRuleType.SFX) { 
            affMap = this.sfxRulesByAffix;
        }
        else if(r.getType() == AffixRuleType.PFX) { 
            affMap = this.pfxRulesByAffix;
        }
        else { 
            return; 
        }
        
        affMap.add(r.getAppend(), r);
    }
    
    private void buildRecursiveRules() {
        for(AffixRule rule : this.rulesByFlag.getAll()) {

            if(rule.getExpansionFlags() == null) { continue; }
            for(String flag : rule.getExpansionFlags()) { // prochazim vsechny additional flagy
                for(AffixRule addRule : this.rulesByFlag.get(flag)) { // pro kazdy flag prochazim pravidla
                    if(addRule == null) { continue; }
                    rule.setAddRuleStraight(addRule); // pridavam straight pravidlo
                    addRule.setAddRuleReverse(rule);  // pridavam reverse pravidlo
                }
            }
        }
    }

    
    private FlagType parseFlagLine(String[] params) throws AffixFormatException {
        if(params[1].equalsIgnoreCase("LONG")) { return FlagType.LONG; }
        else if(params[1].equalsIgnoreCase("NUM")) { return FlagType.NUM; }
        else if(params[1].equalsIgnoreCase("ASCII")) { return FlagType.ASCII; }
        else { throw new AffixFormatException("Unknown flag type: " + params[1]); }
    }

    private AffixRuleBlock parseAffixRuleBlock(String[] params) throws AffixFormatException {
        AffixRuleBlock ret = new AffixRuleBlock();
        if(params.length < 4) { throw new AffixFormatException("Bad affix line format"); }
        ret.flag = params[1];
        ret.crossProduct = params[2].equals("Y");

        try { ret.count = Integer.parseInt(params[3]); }
        catch(NumberFormatException e) { throw new AffixFormatException("Bad lines count for flag: " + ret.flag + ", reason: " + e); }

        if(params[0].equals("SFX")) { ret.type = AffixRuleType.SFX; }
        else if(params[0].equals("PFX")) { ret.type = AffixRuleType.PFX; }

        return ret;
    }
    
    
    private AffixRule parseAffixRuleLine(String[] params, AffixRuleBlock affRuleBlock) throws AffixFormatException {
        if(params.length < 5) { throw new AffixFormatException("Bad affix line format for flag: " + affRuleBlock.flag); }
        if(!params[1].equals(affRuleBlock.flag)) { throw new AffixFormatException("Bad affix file format: flag mismatch: " + affRuleBlock.flag + " vs. " + params[1]); }

        String strType = params[0];
        AffixRuleType type = null;
        if(strType.equals("SFX")) { type = AffixRuleType.SFX; }
        else if(strType.equals("PFX")) { type = AffixRuleType.PFX; }

        if(affRuleBlock.type != type) { throw new AffixFormatException("Bad affix file format: flag mismatch: " + affRuleBlock.flag + " vs. " + params[1]); }
        
        String flag = params[1];
        String remove = params[2].equals("0")?"":params[2];
        String[] append0 = params[3].split("/",2);
        String append;
        if(append0[0].equals("0")) { append = ""; }
        else { append = append0[0]; }
        Set<String> addFlags;
        if(append0.length > 1) { addFlags = this.extractFlags(append0[1]); }
        else { addFlags=null; }

        String condition = params[4];
        String[] properties = (params.length>5)?params[5].split(","):null;

        return new AffixRule(type,flag,remove,append,addFlags,condition,affRuleBlock.crossProduct, properties);
    }
    
    
    public Set<String> extractFlags(String flagString, FlagType flagType) {
        Set<String> ret = new HashSet<String>();
        switch(flagType) {
            case LONG: 
                for(int i=0; i<flagString.length()/2; i++) {
                    String f = flagString.substring(2*i,2*i+2);
                    ret.add(f);
                }
                break;
            case NUM: 
                String[] flags=flagString.split("\\s*,\\s*");
                ret.addAll(Arrays.asList(flags));
                break;
            default:
                for(int i=0; i<flagString.length();i++) {
                    ret.add(flagString.substring(i, i+1));
                }
                break; 
        }
        return ret;
    }
    
    public Set<String> extractFlags(String flagString) {
        return this.extractFlags(flagString, this.flagType);
    }
    
    public Set<String> getAllFlags() {
        return this.rulesByFlag.getKeys();
    }

    public Set<AffixRule> getRulesByFlag(String flag) {
        return this.rulesByFlag.get(flag);
    }
    
    public AffixRuleType getRuleTypeByFlag(String flag) {
        Iterator<AffixRule> it = this.rulesByFlag.iterator(flag);
        if(it == null || !it.hasNext()) return null;
        return it.next().getType();
    }
    
}
