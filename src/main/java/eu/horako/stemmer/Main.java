package eu.horako.stemmer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command line interface for word expansion/stemming.
 * 
 * Use parameter -h from the command line to see the usage.
 * 
 * 
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class Main {
    private final String[] args;

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        Main proc = new Main(args);
        proc.run();
    }
    
    public Main(String[] args) {
        this.args = args;
    }
    
    public void run() {
        if(args.length<3) {
            System.err.println("Usage:");
            System.err.println("processor <affix-file> <dict-file> stem\n");
            System.err.println("processor <affix-file> <dict-file> expand    <depth>\n ");
            System.err.println("processor <affix-file> <dict-file> expandall <depth>\n ");
            System.err.println("processor <affix-file> <dict-file> wordlist  <depth> \n ");
            System.err.println("\nActions:");
            System.err.println("  stem - read words from stdin and print reduced forms (stems/lemmata) to stdout");
            System.err.println("  expand - read basic word forms from stdin and print their expanded forms to stdout");
            System.err.println("  expandall - similar to expand but the source is the dictionary file itself");
            System.err.println("  wordlist - expand words from dictionary with sticky rules and print them");
            
            System.exit(1);
        }
        
        AffixRuleSet ruleSet = null;
        try {
            ruleSet = new AffixRuleSet(args[0]);
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Affix rules loaded");
        } catch (AffixFormatException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read or parse affix file", ex);
            System.exit(1);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read or parse affix file", ex);
            System.exit(1);
        }
        
        Dictionary dictionary = null;
        try {
            dictionary = new Dictionary(args[1],ruleSet);
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Dictionary loaded");
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read dictionary file", ex);
            System.exit(1);
        }
        
        int depth = 3;
        long count = 0;
        
        long startTimeNS = System.nanoTime();
        try {        
            if(args[2].equalsIgnoreCase("wordlist")) {
                count = wordList(ruleSet, dictionary);
            } else if(args[2].equalsIgnoreCase("stem")) { 
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                count = stem(reader,ruleSet,dictionary);
            } else if(args[2].equalsIgnoreCase("expand")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                if(args.length>3) depth = Integer.parseInt(args[3]);
                count = expand(reader, depth, ruleSet, dictionary);
            } else if(args[2].equalsIgnoreCase("expandall")) {
                Iterator<String> it = dictionary.getWords().iterator();
                if(args.length>3) depth = Integer.parseInt(args[3]);
                count = expandall(depth, ruleSet, dictionary);
            } else if(args[2].equalsIgnoreCase("expanddict")) {
                count = expandDict(ruleSet, dictionary);
            } else {
                System.err.println("Unknown mode: " + args[2]);
                System.exit(1);
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        
        long endTimeNS = System.nanoTime();
        
        System.err.println("Processed " + count + " word(s) in " + ((endTimeNS-startTimeNS)/1000.0/1000/1000) + " seconds" );
        
    }
    
    private long expand(BufferedReader reader, int depth, AffixRuleSet ruleSet, Dictionary dictionary) throws IOException {
        AffixExpander expander = new AffixExpander(ruleSet,dictionary); 
        expander.expandStickyRules();
        long count = 0;
        PrintStream output = new PrintStream(System.out,true,"UTF-8");
        while(true) {
            String word = reader.readLine();
            if(word == null) break;
            word = word.trim();
            Set<String> result = expander.expand(word, depth);
            for(String s : result) {
                output.println(word + ":" + s);
            }
            count++;
        }
        return count;
    }
    
    private long expandall(int depth, AffixRuleSet ruleSet, Dictionary dictionary) throws IOException {
        AffixExpander expander = new AffixExpander(ruleSet,dictionary); 
        expander.expandStickyRules();
        PrintStream output = new PrintStream(System.out,true,"UTF-8");
        long count = 0;
        Iterator<String> wordIterator = dictionary.getWords().iterator();
        while(wordIterator.hasNext()) {
            String word = wordIterator.next();
            Set<String> result = expander.expand(word, depth);
            for(String s : result) {
                output.println(word + ":" + s);
            }
            count++;
        }
        return count;
    }

    private long wordList(AffixRuleSet ruleSet, Dictionary dictionary) throws IOException {
        AffixExpander expander = new AffixExpander(ruleSet,dictionary); 
        expander.expandStickyRules();
        PrintStream output = new PrintStream(System.out,true,"UTF-8");
        for(String s : dictionary.getWords()) {
            output.println(s);
        }
        return dictionary.getWords().size();
    }

    private long expandDict(AffixRuleSet ruleSet, Dictionary dictionary) throws IOException {
        AffixExpander expander = new AffixExpander(ruleSet,dictionary); 
        expander.expandStickyRules();
        return dictionary.dump(System.out);
    }

    
    private long stem(BufferedReader reader, AffixRuleSet ruleSet, Dictionary dictionary) throws IOException {
        AffixStemmer stemmer = new AffixStemmer(ruleSet,dictionary); 
        String line;
        PrintStream output = new PrintStream(System.out,true,"UTF-8");
        long count = 0;
        while(true) {
            line = reader.readLine();
            if(line == null) break;
            line = line.trim();
            Set<String> result = stemmer.process(line);
            output.print(line.trim()+":");
            if(result == null) { continue; }
            for(String s : result) {
                output.print(" ");
                output.print(s);
            }
            output.println();
            count++;
        }
        return count;
    }
    
    
    
}

