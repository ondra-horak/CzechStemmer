package eu.horako.stemmer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 * @param <K> key
 * @param <V> value
 */
public class AffixMap<K,V> {
    private final Map<K,Set<V>> affMap = new HashMap<K,Set<V>>();
    private final Set<V> affList = new LinkedHashSet<V>();
    private final Set<V> emptySet = new HashSet<V>();
    
    public void add(K key) {
        if(this.affMap.containsKey(key)) { return; }
        this.affMap.put(key, new LinkedHashSet<V>());
    }
    
    public void add(K key,V value) {
        this.affList.add(value);

        Set<V> list;
        if(this.affMap.containsKey(key)) {
            list = this.affMap.get(key);
        }
        else {
            list = new LinkedHashSet<V>();
            this.affMap.put(key, list);
        }
        list.add(value);
    }
    
    
    public void addAll(AffixMap<K,V> otherMap) {
        this.affList.addAll(otherMap.getAll());
        for(Map.Entry<K,Set<V>> entry : otherMap.affMap.entrySet()) {
            for(V value :  entry.getValue()) {
                this.add(entry.getKey(), value);
            }
        }
    }
    
    public Set<V> get(K key) {
        Set<V> ret =  this.affMap.get(key);
        if(ret == null) return this.emptySet;
        else return ret;
    }
    
    public Set<V> getAll() {
        return this.affList;
    }
    
    public Set<K> getKeys() {
        return this.affMap.keySet();
    }
    
    public Iterator<V> iterator(K key) {
        Collection<V> list = this.get(key);
        if(list == null) { return this.emptySet.iterator(); }
        else { return list.iterator(); }
    }

    public Iterator<V> iterator() {
        return this.affList.iterator();
    }
    
    public boolean isEmpty() {
        return this.affList.isEmpty();
    }
    
    public boolean contains(K key) {
        return this.affMap.containsKey(key);
    }
    
    public Set<Map.Entry<K,Set<V>>> getEntries() {
        return this.affMap.entrySet();
    }
    
    public int size() {
        return this.affList.size();
    }
}
