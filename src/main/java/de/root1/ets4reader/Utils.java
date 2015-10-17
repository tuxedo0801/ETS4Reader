/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of ETS4Reader.
 *
 *   ETS4Reader is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   ETS4Reader is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with ETS4Reader.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.ets4reader;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author achristian
 */
public class Utils {

    static String getKeyForValue(Map<String, List<String>> refMap, String internalId) {
        
        Iterator<String> iterator = refMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            List<String> list = refMap.get(key);
            for (String value : list) {
                if (value.equals(internalId)) {
                    return key;
                }
            }
        }
        return null;
    }
    
    
    
}
