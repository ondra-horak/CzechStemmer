package eu.horako.stemmer;

import gnu.getopt.Getopt;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private final List<String> dictFiles = new ArrayList<String>();
    private final List<String> affixFiles = new ArrayList<String>();
    private int depth = 5;
    private String mode;

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        new Main().run(args);
    }
    
    
    private void help() {
        System.err.println("Usage:");
        System.err.println("processor -m <mode> -d <dict-file> -a <affix-file> ... [-p <depth>]\n");
        System.err.println();
        System.err.println("  -d <dict-file> -a <affix-file> dict/affix file pair; may be repeated;");
        System.err.println("     the dict/affix files are order into pairs (first -d with first -f etc.)");
        System.err.println("  -m <mode> processing mode - one of stem, expand, expandall, wordlist");
        System.err.println();
        System.err.println("  Modes:");
        System.err.println("  stem - read words from stdin and print reduced forms (stems/lemmata) to stdout");
        System.err.println("  expand - read basic word forms from stdin and print their expanded forms to stdout");
        System.err.println("  expandall - similar to expand but the words are taken from the dictionary file itself;");
        System.err.println("  wordlist - expand words from dictionary with sticky rules and print them");
        System.err.println();
        System.err.println("  -p <depth> expansion depth: used for expand and expandall modes; default 5\n");
    }
    
    public void run(String[] args) {
        if(args.length<3) {
            help();
            System.exit(1);
        }
        
        parseOptions(args);
        List<Pair<Dictionary,AffixRuleSet>> dictAffList = loadDictAffixFiles();

        if(dictAffList == null || dictAffList.isEmpty()) {
            System.err.println("ERROR: No dict/affix file pair loaded");
            System.exit(1);
        }
        
        long count = 0;
        long startTimeNS = System.nanoTime();
        BufferedReader reader;
        try {        
            switch(mode) {
                case "wordlist":
                    count = wordList(dictAffList);
                    break;
                case "stem":
                    reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                    count = stem(reader,dictAffList);
                    break;
                case "expand":
                    reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                    count = expand(reader, depth, dictAffList);
                    break;
                case "expandall":
                    count = expandall(depth, dictAffList);
                    break;
                case "expanddict":
                    count = expandDict(dictAffList);
                    break;
                default:
                    System.err.println("Unknown mode: " + mode);
                    System.exit(1);
                    break;
            }
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        
        long endTimeNS = System.nanoTime();
        
        System.err.println("Processed " + count + " word(s) in " + ((endTimeNS-startTimeNS)/1000.0/1000/1000) + " seconds" );
        
    }
   
    
    private List<Pair<Dictionary,AffixRuleSet>> loadDictAffixFiles() {
        List<Pair<Dictionary,AffixRuleSet>> ret = new ArrayList<Pair<Dictionary,AffixRuleSet>>();

        if(dictFiles.size() != affixFiles.size()) {
            System.err.println("ERROR: Counts of dictionary and affix files differ");
            System.exit(1);
        }
        
        for(int i = 0; i < dictFiles.size(); i++) {
            String dictFileName = dictFiles.get(i);
            String affixFileName = affixFiles.get(i);
                    
            AffixRuleSet ruleSet;
            try {
                ruleSet =  new AffixRuleSet(affixFileName);
            } catch (AffixFormatException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read or parse affix file " + affixFileName, ex);
                System.exit(1);
                return null;
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read or parse affix file " + affixFileName, ex);
                System.exit(1);
                return null;
            }
            
            Dictionary dictionary = null;
            try {
                dictionary = new Dictionary(dictFileName,ruleSet);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read dictionary file " + dictFileName, ex);
                System.exit(1);
                return null;
            }
            
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Dictionary/Affix file pair loaded: " + dictFileName + " / " + affixFileName);
            
            ret.add(new Pair<Dictionary,AffixRuleSet>(dictionary,ruleSet));
        }        
        
        return ret;
    }
    
    private void parseOptions(String[] args) {
        Getopt g = new Getopt("processor", args, "f:d:a:p:m:h");
        g.setOpterr(false);

        int opt;
        
        while ((opt = g.getopt()) != -1) {
            switch(opt) {
              case 'h':
                  help();
                  System.exit(1);
                  break;
              case 'm':
                  mode = g.getOptarg().toLowerCase();
                  break;
              case 'd':
                  dictFiles.add(g.getOptarg());
                  break;
              case 'a':
                  affixFiles.add(g.getOptarg());
                  break;
              case 'p':
                  try {
                      depth = Integer.parseInt(g.getOptarg());
                      if(depth <= 0) throw new NumberFormatException();
                  } catch(NumberFormatException ex) {
                      System.err.println("ERROR: depth must be an integer > 0");
                      help();
                      System.exit(1);
                  }
                  break;
              case '?':
                  break; // getopt() already printed an error
              default:
                  System.out.print("getopt() returned " + opt + "\n");
            }
        }
    }
    
    
    private long expand(BufferedReader reader, int depth, List<Pair<Dictionary,AffixRuleSet>> dictAffs) throws IOException {
        List<AffixExpander> expanders = new ArrayList<AffixExpander>();
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            AffixExpander expander = new AffixExpander(dictAff.second,dictAff.first);
            expander.expandStickyRules();
            expanders.add(expander); 
        }
                
        long count = 0;
        PrintStream output = new PrintStream(System.out,true,"UTF-8");
        while(true) {
            String word = reader.readLine();
            if(word == null) break;
            word = word.trim();
            Set<String> result = new HashSet<String>();
            for(AffixExpander expander : expanders) {
                result.addAll(expander.expand(word, depth));
            }
            for(String s : result) {
                output.println(word + ":" + s);
            }
            count++;
        }
        return count;
    }
    
    private long expandall(int depth, List<Pair<Dictionary,AffixRuleSet>> dictAffs) throws IOException {
        List<AffixExpander> expanders = new ArrayList<AffixExpander>();
        long count = 0;
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            AffixExpander expander = new AffixExpander(dictAff.second,dictAff.first);
            expander.expandStickyRules();

            PrintStream output = new PrintStream(System.out,true,"UTF-8");
            Iterator<String> wordIterator = dictAff.first.getWords().iterator();
            while(wordIterator.hasNext()) {
                String word = wordIterator.next();
                Set<String> result = expander.expand(word, depth);
                for(String s : result) {
                    output.println(word + ":" + s);
                }
                count++;
            }
        }
        return count;
    }

    private long wordList(List<Pair<Dictionary,AffixRuleSet>> dictAffs) throws IOException {
        long count = 0;
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            Dictionary dictionary = dictAff.first;
            AffixRuleSet ruleSet = dictAff.second;
            AffixExpander expander = new AffixExpander(ruleSet,dictionary); 
            expander.expandStickyRules();
            PrintStream output = new PrintStream(System.out,true,"UTF-8");
            for(String s : dictionary.getWords()) {
                output.println(s);
            }
            count += dictionary.getWords().size();
        }
        return count;
    }

    private long expandDict(List<Pair<Dictionary,AffixRuleSet>> dictAffs) throws IOException {
        long count = 0;
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            Dictionary dictionary = dictAff.first;
            AffixRuleSet ruleSet = dictAff.second;
            AffixExpander expander = new AffixExpander(ruleSet,dictionary); 
            expander.expandStickyRules();
            count += dictionary.dump(System.out);
        }
        return count;
    }

    
    private long stem(BufferedReader reader, List<Pair<Dictionary,AffixRuleSet>> dictAffs) throws IOException {
        long count = 0;

        List<AffixStemmer> stemmers = new ArrayList<AffixStemmer>();
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            stemmers.add(new AffixStemmer(dictAff.second, dictAff.first));
        }
        
        String line;
        PrintStream output = new PrintStream(System.out,true,"UTF-8");
        while(true) {
            line = reader.readLine();
            if(line == null) break;
            line = line.trim();
            
            Set<String> result = new HashSet<String>();
            for(AffixStemmer stemmer : stemmers) {
                Set<String> r = stemmer.process(line);
                if(r == null) continue;
                result.addAll(r);
            }
            output.print(line + ":");
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

