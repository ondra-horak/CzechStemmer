package eu.horako.stemmer.lucene.fst;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;
import org.apache.lucene.util.packed.PackedInts;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public class StaticFSTBuilder {
    private final BufferedReader reader;
    private String currentStem = null;
    private String[] currentLine = null;
    private int currentIdx = 0;
    private String separator=" ";

    private class StringMultimap extends HashMap<String,Set<String>> {
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
    
    public void setSeparator(String separator) {
        this.separator = separator;
    }
    
    private class StringPair {
        public String key;
        public String value;
        
        public StringPair(String k, String v) {
            key = k;
            value = v;
        }
    }
    
    public StaticFSTBuilder(String fileName) throws FileNotFoundException {
        this.reader = new BufferedReader(new FileReader(fileName));
    }
    
    public StaticFSTBuilder(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    private StringPair getNextPair() throws IOException {
        while(true) {
            if(currentLine == null || currentIdx >= currentLine.length) {
                String line = reader.readLine();
                if(line == null) return null;
                currentLine = line.split(separator);
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
    
    public FST<CharsRef> createFST() throws IOException {
        CharSequenceOutputs outputs = CharSequenceOutputs.getSingleton();
        Builder<CharsRef> builder = 
                new Builder(FST.INPUT_TYPE.BYTE1, 0, 0, true, true, Integer.MAX_VALUE, outputs, false, PackedInts.DEFAULT, true, 15); 
        BytesRefBuilder scratchBytes = new BytesRefBuilder();
        IntsRefBuilder scratchInts = new IntsRefBuilder();
 
        StringMultimap smm = new StringMultimap();
        while(true) {
            StringPair p = getNextPair();
            if(p == null) break;
            smm.put(p.key, p.value);
        }

        for(Map.Entry<String,Set<String>> e : smm.entrySet()) {
            String key = e.getKey();
            String value = join(e.getValue(),separator); 
            scratchBytes.copyChars(key);             
            CharsRefBuilder chrefs = new CharsRefBuilder();
            chrefs.copyChars(value.toCharArray(),0,value.length()); 
            builder.add(Util.toIntsRef(scratchBytes.get(), scratchInts), chrefs.get());
        }

        return builder.finish();        
    }

    public void saveFST(FST<CharsRef> fst, String fileName) throws IOException {
        fst.save(new File(fileName));
    }
}
