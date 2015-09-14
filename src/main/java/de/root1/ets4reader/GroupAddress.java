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

import org.jdom2.Element;

/**
 *
 * @author achristian
 */
public class GroupAddress {
    private final String address;

    private final String name;
    private final String internalId;
    private int mainType;
    private int subType;
    

    GroupAddress(Project project, Element gaElement) {
        name = gaElement.getAttributeValue("Name");
        
        int intAddress = Integer.parseInt(gaElement.getAttributeValue("Address"));
        
        
        int main = intAddress >>> 11 & 0x1F;
        int middle = intAddress >>> 8 & 0x07;
        int sub = intAddress & 0xFF;
        
        address = main+"/"+middle+"/"+sub;
        
        internalId = gaElement.getAttributeValue("Id");
        
    }

    String getInternalId() {
        return internalId;
    }

    @Override
    public String toString() {
        return "GroupAddress{" + "address=" + address + ", name=" + name + ", mainType=" + mainType + ", subType=" + subType + '}';
    }

    /**
     * Get textual representation of group address, f.i. 1/1/100
     * @return 
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get the name of this groupaddress as defined in ETS
     * @return ga name
     */
    public String getName() {
        return name;
    }

    /**
     * Get DPT main type
     * @return f.i. 5
     */
    public int getMainType() {
        return mainType;
    }

    /**
     * Get DPT sub type
     * @return f.i. 0
     */
    public int getSubType() {
        return subType;
    }
    
    void setDataType(int mainType, int subType) {
        this.mainType = mainType;
        this.subType = subType;
    }

    public String getTypeString() {
        return String.format("%d.%03d",mainType, subType);
    }
    
}
