package eu.horako.stemmer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class Dictionary {
    private final Map<String,List<Set<String>>> dictionary = new HashMap<String,List<Set<String>>>();

    public Dictionary() {
    }
    
    public Dictionary(InputStream input, AffixRuleSet affixRuleSet) throws IOException {
        this.load(input, affixRuleSet);
    }
    
    public Dictionary(Reader reader, AffixRuleSet affixRuleSet) throws IOException {
        this.load(reader, affixRuleSet);
    }

    public Dictionary(String filename, AffixRuleSet affixRuleSet) throws IOException {
        InputStream input = new FileInputStream(new File(filename));
        this.load(input, affixRuleSet);
    }

    private void load(InputStream input,AffixRuleSet affixRuleSet) throws IOException {
        InputStreamReader reader = new InputStreamReader(input,"UTF-8");
        this.load(reader, affixRuleSet);
    }
    
    private void load(Reader r,AffixRuleSet affixRuleSet) throws IOException {
        BufferedReader reader = new BufferedReader(r);
        while(true) {
            String line = reader.readLine();
            if(line == null) { break; }
            String[] pair = line.split("/");
            if(pair.length < 2 || pair[0].isEmpty()) { continue; }
            Set<String> flags = affixRuleSet.extractFlags(pair[1]);

            add(pair[0],flags);
        }
        reader.close();
    }
    
    public void add(String word, Set<String> flags) {
            List<Set<String>> fl = this.dictionary.get(word);
            if(fl == null) {
                fl = new ArrayList<Set<String>>(1);
                this.dictionary.put(word.trim(), fl);
            }
            fl.add(flags);
    }
    
    public boolean contains(String word) {
        return this.dictionary.containsKey(word);
    }
    
    public boolean contains(String word, String flag) {
        List<Set<String>> fl = this.dictionary.get(word);
        if(fl == null) { return false; }
        for(Set<String> fs : fl) {
            if (fs.contains(flag)) { return true; }
        }
        return false;
    }

    
   
    public boolean contains(String word, String pfxFlag, String sfxFlag) {
        if(pfxFlag == null) { return this.contains(word,sfxFlag); }
        else if(sfxFlag == null) { return this.contains(word,pfxFlag); }
        
        List<Set<String>> fl = this.dictionary.get(word);
        if(fl == null) { return false; }
        for(Set<String> fs : fl) {
            if (fs.contains(pfxFlag) && fs.contains(sfxFlag)) { return true; }
        }
        return false;
    }
    
    
    public Set<String> getFlags(String word) { // temporary - for backward compatibility with single-flagset version
        List<Set<String>> lf = this.dictionary.get(word);
        if(lf == null) { return null; }
        return lf.get(0); 
    }

    public List<Set<String>> getAllFlags(String word) { // temporary - for backward compatibility with single-flagset version
        return this.dictionary.get(word);
    }
    
    public Set<String> getFlags(String word,int idx) {
        List<Set<String>> lf = this.dictionary.get(word);
        try {
           return lf.get(idx); 
        } catch(Exception e) {
            return null;
        }
    }
    
    public Set<String> getWords() {
        return this.dictionary.keySet();
    }

    public int dump(String fileName) throws IOException {
        return dump(new BufferedOutputStream(new FileOutputStream(new File(fileName))));
    }

    public int dump(OutputStream output) throws IOException {
        PrintStream os = new PrintStream(output,true,"UTF-8");
        
        for(Map.Entry<String,List<Set<String>>> e : dictionary.entrySet()) {
            for(Set<String> flagSet : e.getValue()) {
                os.print(e.getKey());
                os.print("/");
                for(String f : flagSet) {
                    os.print(f);
                }
                os.println();
            }
        }
        return dictionary.entrySet().size();
    }

}
