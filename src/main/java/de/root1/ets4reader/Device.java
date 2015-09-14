/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.ets4reader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;

/**
 *
 * @author achristian
 */
public class Device {

    private final String address;
    private String name;
    /**
     * ComObjInstanceRef-ID <-> GroupAddressRef-ID
     * <br>
     * M-0083_A-0030-20-FCCB_=-30_R-10212 <-> P-05FA-0_GA-246
     */
    private final Map<String, String> refMap = new HashMap<>();

    Device(Project project, int area, int line, Element deviceInstance) {

        String memberAddress = deviceInstance.getAttributeValue("Address");
        if (memberAddress != null) {
            int member = Integer.parseInt(memberAddress);
            address = area + "." + line + "." + member;
        } else {
            address = area + "." + line + ".-";
        }

        name = deviceInstance.getAttributeValue("Name");
        if (name == null) {
            String productRefId = deviceInstance.getAttributeValue("ProductRefId");
//                        deviceName = getDeviceOriginalName(productRefId);
            name = productRefId;
        }

        Element comobjInstanceRefsElement = deviceInstance.getChild("ComObjectInstanceRefs", project.getNamespace());
        if (comobjInstanceRefsElement != null) {
            List<Element> children = comobjInstanceRefsElement.getChildren();
            for (Element comObjectInstanceRefElement : children) {

                String refId = comObjectInstanceRefElement.getAttributeValue("RefId");
                Element connectorsElement = comObjectInstanceRefElement.getChild("Connectors", project.getNamespace());
                if (connectorsElement != null) {
                    List<Element> connectorsChildren = connectorsElement.getChildren();
                    if (!connectorsChildren.isEmpty()) {
                        Element element = connectorsChildren.get(0);
                        String gropAddressRefId = element.getAttributeValue("GroupAddressRefId");
                        refMap.put(refId, gropAddressRefId);
                    }
                }

            }
        }

    }

    /**
     * ComObjInstanceRef-ID <-> GroupAddressRef-ID
     * <br>
     * M-0083_A-0030-20-FCCB_=-30_R-10212 <-> P-05FA-0_GA-246
     *
     * @return
     */
    public Map<String, String> getRefMap() {
        return refMap;
    }

    @Override
    public String toString() {
        return "Device{" + "address=" + address + ", name=" + name + '}';
    }

}
