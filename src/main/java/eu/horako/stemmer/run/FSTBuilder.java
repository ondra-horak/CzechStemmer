package eu.horako.stemmer.run;

import gnu.getopt.Getopt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.lucene.util.BytesRef;

import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;
import org.apache.lucene.util.packed.PackedInts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class FSTBuilder implements IRunner {
    public static Logger logger = LoggerFactory.getLogger(FSTBuilder.class);
    private String currentStem = null;
    private String[] currentLine = null;
    private int currentIdx = 0;
    private String inputSeparator=":";
    private Pattern inputSeparatorPattern;
    private String valuesSeparator=":";
    private Pattern valuesSeparatorPattern;
    private String textFile = null;
    private String fstFile = null;
    private String mode = null;
    private Writer output;

    private class StringMultimap extends TreeMap<String,Set<String>> {
        public void put(String k,String v) {
            Set<String> values = get(k);
            if(values == null) {
                values = new HashSet<String>();
                put(k,values);
            }
            values.add(v);
        }
        
        public String getJoined(String k, String separator) {
            Set<String> values = get(k);
            if(values == null) return null;
            if(values.size()==1) return values.iterator().next();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(String s : values) {
                if(!first) sb.append(separator);
                else first = false;
                sb.append(s);
            }
            return sb.toString();
        }
    }

    @Override
    public void init(String[] args) throws Exception {
        parseOptions(args);
        valuesSeparatorPattern = Pattern.compile(valuesSeparator, Pattern.LITERAL);
        inputSeparatorPattern = Pattern.compile(inputSeparator, Pattern.LITERAL);
    }
    
    @Override
    public void run() throws Exception {
        FST<CharsRef> fst;
        BufferedReader input;
        switch(mode) {
            case "fstbuild":
                if(textFile != null) {
                    input = new BufferedReader(new FileReader(textFile));
                } else {
                    input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
                }
                fst = createFST(input);
                saveFST(fst, fstFile);
                break;
            case "fstcheck":
            case "fstsearch":
                fst = loadFST(fstFile);
                if(textFile != null) {
                    input = new BufferedReader(new FileReader(textFile));
                } else {
                    input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
                }
                output = new OutputStreamWriter(System.out, "UTF-8");
                checkFST(fst, input, output);
                output.close();
                break;
        }
    }
    
    public String join(Collection<String> values, String separator) {
        if(values == null) return null;
        if(values.size()==1) return values.iterator().next();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String s : values) {
            if(!first) sb.append(separator);
            else first = false;
            sb.append(s);
        }
        return sb.toString();
    }
    
    public void setInputSeparator(String s) {
        inputSeparator = s;
    }

    public void setValuesSeparator(String s) {
        valuesSeparator = s;
    }
    
    private class StringPair {
        public String key;
        public String value;

        public StringPair(String k, String v) {
            key = k;
            value = v;
        }
    }

    public FSTBuilder() {
    }


    private StringPair getNextPair(BufferedReader reader) throws IOException {
        while(true) {
            if(currentLine == null || currentIdx >= currentLine.length) {
                String line = reader.readLine();
                if(line == null) return null;
                currentLine = inputSeparatorPattern.split(line);
                if(currentLine.length < 2 || currentLine[0].isEmpty()) { // read next line
                    currentLine = null;
                    continue;
                }
                currentStem = currentLine[0];
                currentIdx = 1;
            }
            String wordForm = currentLine[currentIdx++];
            if(wordForm.isEmpty()) {
                currentIdx++;
                continue;
            }
            return new StringPair(wordForm,currentStem);
        }
    }
    
    
    public FST<CharsRef> createFST(BufferedReader reader) throws IOException {
        CharSequenceOutputs outputs = CharSequenceOutputs.getSingleton();
        Builder<CharsRef> builder = 
                new Builder(FST.INPUT_TYPE.BYTE1, 0, 0, true, true, Integer.MAX_VALUE, outputs, false, PackedInts.DEFAULT, true, 15); 
        BytesRefBuilder scratchBytes = new BytesRefBuilder();
        IntsRefBuilder scratchInts = new IntsRefBuilder();
 
        StringMultimap smm = new StringMultimap();
        while(true) {
            StringPair p = getNextPair(reader);
            if(p == null) break;
            smm.put(p.key, p.value);
        }

        for(Map.Entry<String,Set<String>> e : smm.entrySet()) {
            String key = e.getKey();
            String value = join(e.getValue(),valuesSeparator); 
            scratchBytes.copyChars(key);             
            CharsRefBuilder chrefs = new CharsRefBuilder();
            chrefs.copyChars(value.toCharArray(),0,value.length()); 
            builder.add(Util.toIntsRef(scratchBytes.get(), scratchInts), chrefs.get());
        }

        return builder.finish();        
    }

    public void saveFST(FST<CharsRef> fst, String fileName) throws IOException {
        fst.save(new File(fileName).toPath());
    }



    private void parseOptions(String[] inputArgs) {
        String[] args  = Arrays.copyOf(inputArgs, inputArgs.length);
        Getopt g = new Getopt("processor", args, "i:t:f:I:T:F:m:h");
        g.setOpterr(false);

        int opt;
        boolean printHelp = false;
        while ((opt = g.getopt()) != -1) {
            switch(opt) { 
              case 'h':
                  printHelp = true;
                  break;
              case 'i':
              case 't':
                  textFile = g.getOptarg();
                  break;
              case 'f':
                  fstFile = g.getOptarg();
                  break;
              case 'I':
              case 'T':
                  inputSeparator = g.getOptarg();
                  break;
              case 'F':
                  valuesSeparator = g.getOptarg();
                  break;
              case 'm':
                  mode = g.getOptarg();
              case '?':
                  break; // getopt() already printed an error
              default:
                  System.out.print("getopt() returned " + opt + "\n");
            }
        }
        if(printHelp) {
            if("fstsearch".equals(mode)) {
                mode = "fstcheck";
            }
            String helpFile = mode == null ? "help.txt" : ("help-" + mode + ".txt");
            Main.printResourceToStderr(helpFile);
            System.exit(1);
        }
    }
    
    private void checkFST(FST<CharsRef> fst, BufferedReader input, Writer output) throws IOException {
        while(true) {
            String s = input.readLine();
            if(s == null) {
                break;
            }
            List<String> values = getValuesFromFST(s, fst);
            if(values.isEmpty()) {
                output.write(s);
                output.write("\n");
            } else {
                for(String v : values) {
                    output.write(s);
                    output.write(inputSeparator);
                    output.write(v);
                    output.write("\n");
                }
            }
        }
        
    }
    
    private FST<CharsRef> loadFST(String fname) throws IOException {
        CharSequenceOutputs outputs = CharSequenceOutputs.getSingleton();
        FST<CharsRef> fst = (FST<CharsRef>)FST.read(new File(fname).toPath(), outputs);
        return fst;
    }

    private List<String> getValuesFromFST(String key, FST<CharsRef> fst) {
        try {
            CharsRef charsRef = Util.get(fst, new BytesRef(key));
            if(charsRef == null) {
                return new ArrayList<>();
            }
            return Arrays.asList(valuesSeparatorPattern.split(charsRef.toString()));
        } catch (IOException ex) {
            logger.warn("Cannot get string from FST (key='" + key + "'", ex);
            return new ArrayList<>();
        }
        
    }

}
