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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class KnxProjReader {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Pattern projectPattern = Pattern.compile("P-[0-9A-F]{4}");
    private final Pattern manufacturerPattern = Pattern.compile("M-[0-9A-F]{4}");

    // M-0083_A-004D-12-E268
    // M-00C8_A-2820-40-090B-O00C5
    private final Pattern manufacturerDevicePattern = Pattern.compile("M-[0-9A-F]{4}_[0-9A-F]-[0-9A-F]{4}-[0-9A-F]{2}-[0-9A-F]{4}(-[0-9A-Z]{1}[0-9A-F]{4})?");

    private final List<Project> projects = new ArrayList<>();

    /**
     * Starts reading the project. This might take some time ...
     *
     * @param knxprojFile
     * @throws IOException
     * @throws JDOMException
     */
    public KnxProjReader(File knxprojFile) throws IOException, JDOMException {
        File tmpFolder = createTempDirectory();
        log.debug("Extracting to {}", tmpFolder.getCanonicalPath());
        extract(knxprojFile, tmpFolder);

        readProjects(tmpFolder);
        readDPT(tmpFolder);
        readUserConfiguration(knxprojFile);

        for (Project project : projects) {
            for (Device device : project.getDeviceList()) {
                log.debug("Found device: {}", device);
            }
            for (GroupAddress groupAddress : project.getGroupaddressList()) {
                log.debug("Found groupaddress: {}", groupAddress);
            }
        }
        /*
         log.debug("Deleting temp files {}" + tmpFolder.getAbsolutePath());
         Path directory = Paths.get(tmpFolder.toURI());
         Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
         log.trace("delete file: {}", file);
         Files.delete(file);
         return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
         log.trace("del direte: {}", dir);
         Files.delete(dir);
         return FileVisitResult.CONTINUE;
         }

         });
         log.debug("Deleting temp files *DONE*");
         */
    }

    /**
     * Gets a list of projects. Typically the list has size=1
     *
     * @return list of projects found in .knxproj file
     */
    public List<Project> getProjects() {
        return projects;
    }

    public static File createTempDirectory() throws IOException {
        final File temp;

        temp = File.createTempFile("KnxProjReader", Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }

    private void extract(File knxprojfile, File targetDir) {
        try {
            // Open the zip file
            try (ZipFile zipFile = new ZipFile(knxprojfile)) {
                Enumeration<?> enu = zipFile.entries();
                while (enu.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) enu.nextElement();

                    String name = zipEntry.getName();
                    long size = zipEntry.getSize();
                    long compressedSize = zipEntry.getCompressedSize();
                    log.debug(String.format("name: %-20s | size: %6d | compressed size: %6d\n",
                            name, size, compressedSize));

                    // Do we need to create a directory ?
                    File file = new File(targetDir, name);
                    if (name.endsWith("/")) {
                        file.mkdirs();
                        continue;
                    }

                    File parent = file.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }

                    FileOutputStream fos;
                    // Extract the file
                    try (InputStream is = zipFile.getInputStream(zipEntry)) {
                        fos = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = is.read(bytes)) >= 0) {
                            fos.write(bytes, 0, length);
                        }
                    }
                    fos.close();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readProjects(File tmpFolder) throws IOException, JDOMException {
        File[] projFolders = tmpFolder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    String name = pathname.getName();
                    Matcher matcher = projectPattern.matcher(name);
                    return matcher.find();

                }
                return false;
            }
        });

        for (File projFolder : projFolders) {
            Project project = new Project(projFolder);
            log.info("Found project: {}", project);
            projects.add(project);
        }

    }

    private void readDPT(File tmpFolder) throws JDOMException, IOException {
        /**
         * devicefile <-> commObjRefDptCache
         */
        Map<String, Map<String, String>> manufacturerCache = new HashMap<>();

        for (Project project : projects) {

            for (GroupAddress groupAddress : project.getGroupaddressList()) {
                String internalId = groupAddress.getInternalId();

                boolean connectedToDevice = false;

                for (Device device : project.getDeviceList()) {
                    if (connectedToDevice) {
                        break;
                    }
                    Map<String, List<String>> refMap = device.getRefMap();

                    String comObjInstanceRef = Utils.getKeyForValue(refMap, internalId);

                    
                    if (comObjInstanceRef != null && !connectedToDevice) {
                        connectedToDevice = true;
                        // It's a matching device
                        String dpt = device.getDptMap().get(comObjInstanceRef);

                        if (dpt == null) {

                            // extract manufacturer refId (which is folder name)
                            String manufacturerId;
                            Matcher matcher = manufacturerPattern.matcher(comObjInstanceRef);
                            if (matcher.find()) {
                                manufacturerId = comObjInstanceRef.substring(matcher.start(), matcher.end());
                            } else {
                                log.error("No manufacturer found for {}", comObjInstanceRef);
                                manufacturerId = "";
                            }

                            // extract device xml file
                            String deviceFileName;
                            Matcher deviceMatcher = manufacturerDevicePattern.matcher(comObjInstanceRef);
                            if (deviceMatcher.find()) {
                                deviceFileName = comObjInstanceRef.substring(deviceMatcher.start(), deviceMatcher.end());
                            } else {
                                log.error("No device found for {}", comObjInstanceRef);
                                deviceFileName = "";
                            }

                            Map<String, String> cache = manufacturerCache.get(deviceFileName);

                            if (cache == null) {
                                File mFolder = new File(tmpFolder, manufacturerId);
                                File mFile = new File(mFolder, deviceFileName + ".xml");
                                log.debug("Create cache for " + deviceFileName);
                                cache = createCache(mFile);
                                log.debug("Create cache for {}", deviceFileName + " ... *DONE*");
                                manufacturerCache.put(deviceFileName, cache);

                            } else {
                                log.debug("Using cache for {}", deviceFileName);
                            }

                            dpt = cache.get(comObjInstanceRef);
                            log.debug("Found device defined DPT '{}' for GA {}", dpt, groupAddress.getAddress());
                        } else {
                            log.debug("Found ETS defined DPT '{}' for GA {}", dpt, groupAddress.getAddress());
                        }

                        if (dpt != null) {

                            dpt = dpt.split(" ")[0];
                            String[] split = dpt.split("-");
                            if (split[0].equals("DPST")) {
                                int mainType = Integer.parseInt(split[1]);
                                int subType = Integer.parseInt(split[2]);
                                groupAddress.setDataPointType(mainType, subType);
                            } else if (split[0].equals("DPT")) {
                                int mainType = Integer.parseInt(split[1]);
                                int subType = 0;
                                groupAddress.setDataPointType(mainType, subType);
                            }
                        } else {
                            log.warn(">>>>>> Groupaddress {} has no DPT! Please configure in ETS! <<<<<<", groupAddress.getAddress());
                        }

                    }
                }
                if (!connectedToDevice) {
                    log.debug(">>>>>> Groupaddress {} is not connected to any device in ETS! <<<<<<", groupAddress.getAddress());
                }
                groupAddress.setConnected(connectedToDevice);
            }

        }
    }

    private Map<String, String> createCache(File mFile) throws JDOMException, IOException {

        // ComObjectRef -> DPT
        Map<String, String> cache = new HashMap<>();

        SAXBuilder builder = new SAXBuilder();

        Document document = (Document) builder.build(mFile);
        Element rootElement = document.getRootElement();
        Namespace ns = rootElement.getNamespace();

        // ComObject ID -> DPT
        Map<String, String> comObjectDptCache = new HashMap<>();

        Element applicationProgramsElement = rootElement.getChild("ManufacturerData", ns).getChild("Manufacturer", ns).getChild("ApplicationPrograms", ns);

        // Cache all comobject's DPTs
        Element comObjectTable = applicationProgramsElement.getChild("ApplicationProgram", ns).getChild("Static", ns).getChild("ComObjectTable", ns);
        List<Element> comObjects = comObjectTable.getChildren("ComObject", ns);
        for (Element comObject : comObjects) {
            String id = comObject.getAttributeValue("Id");
            String dpt = comObject.getAttributeValue("DatapointType");
            if (dpt == null) {
                log.debug("ManufacturerDevice File {} comobject id={} has no DPT?!", mFile.getAbsolutePath(), id);
            }
            comObjectDptCache.put(id, dpt);
        }

        Element comObjectRefs = applicationProgramsElement.getChild("ApplicationProgram", ns).getChild("Static", ns).getChild("ComObjectRefs", ns);
        List<Element> children = comObjectRefs.getChildren("ComObjectRef", ns);
        for (Element comObjectRefElement : children) {
            String refId = comObjectRefElement.getAttributeValue("Id");
            String comObjectId = comObjectRefElement.getAttributeValue("RefId");

            String dpt = comObjectRefElement.getAttributeValue("DatapointType");
            if (dpt == null) {
                // ask comobject cache
                dpt = comObjectDptCache.get(comObjectId);
            }

            if (dpt == null) {
                log.debug("ComObjRef '{}' has no DPT??? file: {}", refId, mFile.getAbsolutePath());
            }

            cache.put(refId, dpt);
        }

        return cache;
    }

    private void readUserConfiguration(File knxprojFile) {
        try {
            File userConfigFile = new File(knxprojFile.getAbsolutePath() + ".user.xml");

            Project project = projects.get(0);

            List<GroupAddress> gaWithMissingConfig = new ArrayList<>();

            // get unconnected or dpt-undefined GAs
            List<GroupAddress> groupaddressList = project.getGroupaddressList();
            for (GroupAddress ga : groupaddressList) {
                if (!ga.isConnected() || ga.getMainType() == GroupAddress.UNSPECIFIED) {
                    log.info("{} has missing DPT or is unconnected", ga);
                    gaWithMissingConfig.add(ga);
                }
            }


            boolean needToSafe = false;
            
            SAXBuilder builder = new SAXBuilder();
            Document document;
            if (userConfigFile.exists()) {
                document = (Document) builder.build(userConfigFile);
            } else {
                document = new Document(new Element("knxprojectuserconfiguration"));
                needToSafe = true;
            }
            
            Element rootElement = document.getRootElement();

            // Get already known GAs from .knxproj.user.xml File --> kind of cache
            List<String> alreadyKnownUserConfigGa = new ArrayList<>();
            List<Element> gaElements = rootElement.getChildren("ga");
            for (Element gaElement : gaElements) {
                alreadyKnownUserConfigGa.add(gaElement.getAttributeValue("address"));
            }


            if (!gaWithMissingConfig.isEmpty()) {
                for (GroupAddress ga : gaWithMissingConfig) {

                    if (!alreadyKnownUserConfigGa.contains(ga.getAddress())) {
                        Element gaElement = new Element("ga");
                        
                        gaElement.addContent(new Comment(ga.getName()));
                        gaElement.setAttribute("address", ga.getAddress());
                        gaElement.setAttribute("dpt", "");
                        rootElement.addContent(gaElement);
                        needToSafe = true;
                    }

                }

            }
            
            

            List<Element> children = rootElement.getChildren("ga");
            for (Element gaElement : children) {
                String address = gaElement.getAttributeValue("address");
                
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
                // TODO: TO BE TESTED!
                if (System.getProperty("purge")!=null) {
                    GroupAddress ga = project.getGroupAddress(address);
                    if (ga.isConnected() && ga.getMainType()!=GroupAddress.UNSPECIFIED) {
                        rootElement.removeContent(gaElement);
                        log.info("PURGE: Removing GA={} from userconfig because of ETS config now available");
                        needToSafe = true;
                        continue;
                    }
                }
                
                String dpt = gaElement.getAttributeValue("dpt");

                if (dpt != null && dpt.isEmpty()) {
                    dpt = null;
                }
                String name = gaElement.getAttributeValue("name");
                if (name != null && name.isEmpty()) {
                    name = null;
                }

                GroupAddress groupAddress = project.getGroupAddress(address);

                if (groupAddress != null && dpt != null) {

                    String[] split = dpt.split("\\.");

                    groupAddress.setDataPointType(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                    groupAddress.setConnected(true);

                    if (name != null) {
                        groupAddress.setName(name);
                    }
                    groupAddress.setUserConfigured(true);
                } else if (dpt!=null){
                    groupAddress = new GroupAddress(address, dpt, name);
                    groupAddress.setConnected(true);
                    project.addGroupAddress(groupAddress);
                    groupAddress.setUserConfigured(true);
                }
            }
            
            if (needToSafe) {
                // new XMLOutputter().output(doc, System.out);
                XMLOutputter xmlOutput = new XMLOutputter();

                // display nice nice
                xmlOutput.setFormat(Format.getPrettyFormat());
                xmlOutput.output(document, new FileWriter(userConfigFile));
            }

        } catch (JDOMException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
