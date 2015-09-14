/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.ets4reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;
import javax.xml.bind.DatatypeConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class Project {
    
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String projectfile = "Project.xml";
    private static final String datafile = "0.xml";

    private final String internalID;
    private final String name;
    private final Calendar lastModified;
    private final Calendar projectStart;
    private final List<Device> deviceList = new ArrayList<>();
    private final List<GroupAddress> groupaddressList = new ArrayList<>();

    private final File projTmpFolder;
    private Namespace ns;

    Project(File projFolder) throws IOException, JDOMException {

        projTmpFolder = projFolder;
        SAXBuilder builder = new SAXBuilder();

        Document document = (Document) builder.build(new File(projFolder, projectfile));
        Element rootElement = document.getRootElement();
        Namespace ns = rootElement.getNamespace();

//        for (Element element : rootElement.getChildren()) {
//            log.debug(element);
//        }
        Element projectElement = rootElement.getChild("Project", ns);

        internalID = projectElement.getAttributeValue("Id");

        Element projInfoElement = projectElement.getChild("ProjectInformation", ns);
        name = projInfoElement.getAttributeValue("Name");
        lastModified = DatatypeConverter.parseDateTime(projInfoElement.getAttributeValue("LastModified"));
        projectStart = DatatypeConverter.parseDateTime(projInfoElement.getAttributeValue("ProjectStart"));

        readProjectData();

    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return "Project{" + "internalID=" + internalID + ", name=" + name + ", lastModified=" + lastModified.getTime() + ", projectStart=" + projectStart.getTime() + '}';
    }

    private void readProjectData() throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();

        Document document = (Document) builder.build(new File(projTmpFolder, datafile));
        Element rootElement = document.getRootElement();
        ns = rootElement.getNamespace();

        Element projectElement = rootElement.getChild("Project", ns);

        if (projectElement.getAttributeValue("Id").equals(internalID)) {
            Element installationsElement = projectElement.getChild("Installations", ns);

            List<Element> installations = installationsElement.getChildren("Installation", ns);

            for (Element installationElement : installations) {

                // read devices
                Element topologyElement = installationElement.getChild("Topology", ns);
                readDevices(topologyElement, ns);

                // read groupaddresses
                Element groupaddressesElement = installationElement.getChild("GroupAddresses", ns);
                readGroupAdresses(groupaddressesElement, ns);

            }

        }
        
    }

    public List<GroupAddress> getGroupaddressList() {
        return groupaddressList;
    }
    
    public GroupAddress getGroupAddress(String ga) {
        for (GroupAddress groupaddress : groupaddressList) {
            if (groupaddress.getAddress().equals(ga)) {
                return groupaddress;
            }
        }
        return null;
    }

    public List<Device> getDeviceList() {
        return deviceList;
    }
    

    /**
     * <pre>
     * Topology
     *   Area
     *     Line
     *       DeviceInstance
     * </pre>
     *
     * @param topologyElement
     * @param ns
     */
    private void readDevices(Element topologyElement, Namespace ns) {

        List<Element> areas = topologyElement.getChildren("Area", ns);
        for (Element area : areas) {

            int areaValue = Integer.parseInt(area.getAttributeValue("Address"));

            List<Element> lines = area.getChildren("Line", ns);

            for (Element line : lines) {

                int lineValue = Integer.parseInt(line.getAttributeValue("Address"));

                List<Element> devices = line.getChildren("DeviceInstance", ns);
                for (Element device : devices) {

                    Device d = new Device(this, areaValue, lineValue, device);
                    log.debug("Found device: {}",d);
                    deviceList.add(d);

                }

            }

        }

    }

    /**
     * <pre>
     * GroupAddresses
     *   GroupRanges
     *     GroupRange       <-- Main
     *       GroupRange         <-- Sub
     *         GroupAddress         <-- GA
     * </pre> @param groupaddressesElement
     * @param ns
     */
    private void readGroupAdresses(Element groupaddressesElement, Namespace ns) {

        Stack<Element> stack = new Stack<>();

        stack.push(groupaddressesElement);

        while (!stack.isEmpty()) {
            Element element = stack.pop();
            List<Element> children = element.getChildren();
            if (!children.isEmpty()) {
                for (Element child : children) {
                    stack.push(child);

                }
            } else if (element.getName().equals("GroupAddress")) {
                GroupAddress ga = new GroupAddress(this, element);
                log.debug("Found GroupAddress: {}",ga);
                groupaddressList.add(ga);
            }
        }
        
    }

    Namespace getNamespace() {
        return ns;
    }

}
