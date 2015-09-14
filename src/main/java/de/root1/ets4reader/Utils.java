/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.ets4reader;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author achristian
 */
public class Utils {
    
    public static <KEY, VALUE> KEY  getKeyForValue(Map<KEY,VALUE> map, VALUE value){
        
        Set<Map.Entry<KEY, VALUE>> entrySet = map.entrySet();
        
        for (Map.Entry<KEY, VALUE> entry : entrySet) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
}
