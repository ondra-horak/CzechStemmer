package eu.horako.stemmer.run;

import eu.horako.stemmer.AffixExpander;
import eu.horako.stemmer.AffixFormatException;
import eu.horako.stemmer.AffixRuleSet;
import eu.horako.stemmer.AffixStemmer;
import eu.horako.stemmer.Dictionary;
import eu.horako.stemmer.Pair;
import gnu.getopt.Getopt;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
    protected String exceptionsFile = null;
    private int depth = 5;
    private String mode;
    private final String expandSeparator = ":";

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
            {
                reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
                Set<String> exceptions = loadExceptions(exceptionsFile);
                count = expand(reader, depth, dictAffList, exceptions);
                break;
            }
            case "expandall":
            {
                Set<String> exceptions = loadExceptions(exceptionsFile);
                count = expandall(depth, dictAffList, exceptions);
                break;
            }
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
        Getopt g = new Getopt("processor", args, "d:a:m:p:e:lh");
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
              case 'e':
                  exceptionsFile = g.getOptarg();
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
            } catch (AffixFormatException | IOException ex) {
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



    private long expand(BufferedReader reader, int depth, List<Pair<Dictionary,AffixRuleSet>> dictAffs, Set exceptions) throws IOException {
        List<AffixExpander> expanders = new ArrayList<AffixExpander>();
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            AffixExpander expander = new AffixExpander(dictAff.second,dictAff.first);
            expander.expandStickyRules();
            expanders.add(expander); 
        }
                
        long count = 0;
        OutputStreamWriter writer = new OutputStreamWriter(System.out, "UTF-8");
        while(true) {
            String word = reader.readLine();
            if(word == null) break;
            word = lowerCase ? word.trim().toLowerCase() : word.trim();
            Set<String> result = new HashSet<String>();
            for(AffixExpander expander : expanders) {
                result.addAll(expander.expand(word, depth));
            }
            for(String s : result) {
                String outputStr = word + expandSeparator + s;
                if(!exceptions.contains(outputStr)) {
                    writer.write(outputStr);
                    writer.write('\n');
                }
            }
            writer.flush();
            count++;
        }
        return count;
    }
    
    private long expandall(int depth, List<Pair<Dictionary,AffixRuleSet>> dictAffs, Set exceptions) throws IOException {
        long count = 0;
        for(Pair<Dictionary,AffixRuleSet> dictAff : dictAffs) {
            AffixExpander expander = new AffixExpander(dictAff.second,dictAff.first);
            expander.expandStickyRules();

            OutputStreamWriter writer = new OutputStreamWriter(System.out, "UTF-8");
            Iterator<String> wordIterator = dictAff.first.getWords().iterator();
            while(wordIterator.hasNext()) {
                String word = wordIterator.next();
                Set<String> result = expander.expand(word, depth);
                for(String s : result) {
                    String outputStr = word + expandSeparator + s;
                    if(!exceptions.contains(outputStr)) {
                        writer.write(outputStr);
                        writer.write('\n');
                    }
                }
                count++;
            }
            writer.flush();
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
            OutputStreamWriter writer = new OutputStreamWriter(System.out, "UTF-8");
            for(String s : dictionary.getWords()) {
                writer.write(s);
                writer.write('\n');
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
        OutputStreamWriter writer = new OutputStreamWriter(System.out, "UTF-8");
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
            writer.write(line + ":");
            for(String s : result) {
                writer.write(" ");
                writer.write(s);
            }
            writer.write('\n');
            count++;
        }
        return count;
    }
    
    private Set<String> loadExceptions(String fileName) {
        Set<String> result = new HashSet<>();
        if(fileName == null) {
            return result;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"UTF-8"));
            while(true) {
                String line = reader.readLine();
                if(line == null) { break; }
                String[] pair = line.split(expandSeparator);
                if(pair.length < 2) {
                    continue;
                }
                String lemma = pair[0].trim();
                String form = pair[1].trim();
                if(lemma.isEmpty() || form.isEmpty()) {
                    continue;
                }
                result.add(lemma + expandSeparator + form);
            }
            reader.close();
            return result;
        } catch(IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Cannot read exceptions file " + fileName, ex);
            System.exit(1);
            return null;
        }
    }
}
