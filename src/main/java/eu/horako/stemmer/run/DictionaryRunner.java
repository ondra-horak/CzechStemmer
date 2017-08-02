package eu.horako.stemmer.run;

import eu.horako.stemmer.AffixExpander;
import eu.horako.stemmer.AffixFormatException;
import eu.horako.stemmer.AffixRuleSet;
import eu.horako.stemmer.AffixStemmer;
import eu.horako.stemmer.Dictionary;
import eu.horako.stemmer.Pair;
import gnu.getopt.Getopt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 * 
 */
public class DictionaryRunner implements IRunner {
    protected final List<String> dictFiles = new ArrayList<String>();
    protected final List<String> affixFiles = new ArrayList<String>();
    protected List<Pair<Dictionary,AffixRuleSet>> dictAffList;
    protected boolean lowerCase = false;
    private int depth = 5;
    private String mode;

    @Override
    public void init(String[] args) throws Exception {
        parseOptions(args);
        dictAffList = loadDictAffixFiles();
        if(dictAffList == null || dictAffList.isEmpty()) {
            System.err.println("ERROR: No dict/affix file pair loaded");
            System.exit(1);
        }
    }

    @Override
    public void run() throws Exception {
        long count;
        BufferedReader reader;
        switch(mode) {
            case "expand":
                reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                count = expand(reader, depth, dictAffList);
                break;
            case "expandall":
                count = expandall(depth, dictAffList);
                break;
            case "wordlist":
                count = wordList(dictAffList);
                break;
            case "expanddict":
                count = expandDict(dictAffList);
                break;
            case "stem":
                reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                count = stem(reader,dictAffList);
                break;
            default:
                    System.err.println("Unknown mode: " + mode);
                    System.exit(1);
                    break;
        }
    }

    private void parseOptions(String[] inputArgs) {
        String[] args  = Arrays.copyOf(inputArgs, inputArgs.length);
        Getopt g = new Getopt("processor", args, "d:a:m:p:lh");
        g.setOpterr(true);

        int opt;
        boolean printHelp = false;
        while ((opt = g.getopt()) != -1) {
            switch(opt) {
              case 'h':
                  printHelp = true;
                  break;
              case 'm':
                  mode = g.getOptarg().toLowerCase();
                  break;
              case 'l':
                  lowerCase = true;
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
                      System.exit(1);
                  }
                  break;
              case '?':
                  break;
              default:
                  break;
            }
        }
        
        if(printHelp) {
            String helpFile = mode == null ? "help.txt" : ("help-" + mode + ".txt");
            Main.printResourceToStderr(helpFile);
            System.exit(1);
        }
    }

    private List<Pair<Dictionary,AffixRuleSet>> loadDictAffixFiles() {
        List<Pair<Dictionary,AffixRuleSet>> ret = new ArrayList<Pair<Dictionary,AffixRuleSet>>();

        if(dictFiles.size() != affixFiles.size()) {
            System.err.println("ERROR: Counts of dictionary and affix files differ " + dictFiles.size() + ", " + affixFiles.size());
            System.exit(1);
        }
        
        for(int i = 0; i < dictFiles.size(); i++) {
            String dictFileName = dictFiles.get(i);
            String affixFileName = affixFiles.get(i);
                    
            AffixRuleSet ruleSet;
            try {
                ruleSet =  new AffixRuleSet(affixFileName, lowerCase);
            } catch (AffixFormatException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read or parse affix file " + affixFileName, ex);
                System.exit(1);
                return null;
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read or parse affix file " + affixFileName, ex);
                System.exit(1);
                return null;
            }
            
            Dictionary dictionary;
            try {
                dictionary = new Dictionary(dictFileName, ruleSet, lowerCase);
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
            word = lowerCase ? word.trim().toLowerCase() : word.trim();
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
            line = lowerCase ? line.trim().toLowerCase() : line.trim() ;
            
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
