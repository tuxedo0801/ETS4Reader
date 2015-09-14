/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public int getMainType() {
        return mainType;
    }

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
