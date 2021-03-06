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

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.JDOMException;

/**
 *
 * @author achristian
 */
public class ETS4Reader {
    
    public static void main(String[] args) throws IOException, JDOMException {
        KnxProjReader knxProjReader = new KnxProjReader(new File(args[0]));
        
        List<GroupAddress> groupaddressList = knxProjReader.getProjects().get(0).getGroupaddressList();
        
        for (GroupAddress ga : groupaddressList) {
            System.out.println(ga);
        }
    }
    
}
