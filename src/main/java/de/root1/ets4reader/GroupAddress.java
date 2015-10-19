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
    
    public static final int UNSPECIFIED = -1;

    private String name;
    private final String internalId;
    private int mainType=UNSPECIFIED;
    private int subType=UNSPECIFIED;
    private boolean connected;
    private boolean userConfigured;
    

    GroupAddress(Element gaElement) {
        name = gaElement.getAttributeValue("Name");
        
        int intAddress = Integer.parseInt(gaElement.getAttributeValue("Address"));
        
        
        int main = intAddress >>> 11 & 0x1F;
        int middle = intAddress >>> 8 & 0x07;
        int sub = intAddress & 0xFF;
        
        address = main+"/"+middle+"/"+sub;
        
        internalId = gaElement.getAttributeValue("Id");
        
    }

    GroupAddress(String address, String dpt, String name) {
        String[] split = dpt.split("\\.");
        mainType = Integer.parseInt(split[0]);
        subType = Integer.parseInt(split[1]);
        this.address = address;
        this.name = name;
        internalId = "USERCONFIG";
    }

    String getInternalId() {
        return internalId;
    }

    @Override
    public String toString() {
        return "GroupAddress{" + "address=" + address + ", name=" + name + ", userconfig="+userConfigured+", connected="+isConnected()+", mainType=" + mainType + ", subType=" + subType + '}';
    }

    /**
     * Get textual representation of group address, f.i. 1/1/100
     * @return address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get the name of this groupaddress as defined in ETS. If not defined, address is returned
     * @return ga name
     */
    public String getName() {
        return name!=null?name:getAddress();
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
    
    void setDataPointType(int mainType, int subType) {
        this.mainType = mainType;
        this.subType = subType;
    }

    /**
     * DPT String, like "1.001"
     * @return DPT
     */
    public String getDataPointType() {
        return String.format("%d.%03d",mainType, subType);
    }

    void setConnected(boolean connectedToDevice) {
        this.connected = connectedToDevice;
    }

    /**
     * Returns whether the GA is connected to a device or not.
     * @return GA is connected with a device in ETS, or not
     */
    public boolean isConnected() {
        return connected;
    }

    void setName(String name) {
        this.name = name;
    }

    public boolean isUserConfigured() {
        return userConfigured;
    }

    void setUserConfigured(boolean userConfigured) {
        this.userConfigured = userConfigured;
    }
    
    
    
}
