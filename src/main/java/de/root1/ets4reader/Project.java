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

        File projectFile = new File(projFolder, projectfile);
        if (!projectFile.exists()) {
            projectFile = new File(projFolder, projectfile.toLowerCase());
            log.debug("Using lower case project file name");
        }
        
        Document document = (Document) builder.build(projectFile);
        
        Element rootElement = document.getRootElement();
        Namespace ns = rootElement.getNamespace();

//        for (Element element : rootElement.getChildren()) {
//            log.debug(element);
//        }
        Element projectElement = rootElement.getChild("Project", ns);

        internalID = projectElement.getAttributeValue("Id");

        Element projInfoElement = projectElement.getChild("ProjectInformation", ns);
        name = projInfoElement.getAttributeValue("Name");
        String lastModifiedString = projInfoElement.getAttributeValue("LastModified");
        String projectStartString = projInfoElement.getAttributeValue("ProjectStart");
        Calendar unknown = Calendar.getInstance();
        unknown.setTimeInMillis(0);
        lastModified = lastModifiedString != null && !lastModifiedString.isEmpty() ? DatatypeConverter.parseDateTime(lastModifiedString) : unknown;
        projectStart = projectStartString != null && !projectStartString.isEmpty() ? DatatypeConverter.parseDateTime(projectStartString) : unknown;
        readProjectData();

    }

    /**
     * get the name of the projects as defined in ETS
     *
     * @return project's name
     */
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

    /**
     * Get the group addresses used in this project, as defined in ETS
     *
     * @return list of group addresses
     */
    public List<GroupAddress> getGroupaddressList() {
        return groupaddressList;
    }

    /**
     * get a specific group address by given Group Address name string
     *
     * @param ga name of the group address as defined in ETS, f.i. "Livingroom
     * Light"
     * @return the group address for the given name or null if not found
     */
    public GroupAddress getGroupAddress(String ga) {
        for (GroupAddress groupaddress : groupaddressList) {
            if (groupaddress.getAddress().equals(ga)) {
                return groupaddress;
            }
        }
        return null;
    }

    /**
     * Returns a list of devices used in this project
     *
     * @return list of devices
     */
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
                    log.debug("Found device: {}", d);
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
     *
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
                GroupAddress ga = new GroupAddress(element);
                log.debug("Found GroupAddress: {}", ga);
                groupaddressList.add(ga);
            }
        }

    }

    Namespace getNamespace() {
        return ns;
    }

    void addGroupAddress(GroupAddress groupAddress) {
        groupaddressList.add(groupAddress);

    }

}
