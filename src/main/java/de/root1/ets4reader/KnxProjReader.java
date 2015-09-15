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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
public class KnxProjReader {
    
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final Pattern projectPattern = Pattern.compile("P-[0-9A-F]{4}");
    private final Pattern manufacturerPattern = Pattern.compile("M-[0-9A-F]{4}");

    // M-0083_A-004D-12-E268
    private final Pattern manufacturerDevicePattern = Pattern.compile("M-[0-9A-F]{4}_[0-9A-F]-[0-9A-F]{4}-[0-9A-F]{2}-[0-9A-F]{4}");

    private final List<Project> projects = new ArrayList<>();

    public KnxProjReader(File knxprojFile) throws IOException, JDOMException {
        File tmpFolder = createTempDirectory();
        log.debug("Extracting to {}", tmpFolder.getCanonicalPath());
        extract(knxprojFile, tmpFolder);

        readProjects(tmpFolder);

        readDPT(tmpFolder);
        
        for (Project project : projects) {
            for (Device device : project.getDeviceList()){
                log.debug("Found device: {}", device);
            }
            for(GroupAddress groupAddress : project.getGroupaddressList()){
                log.debug("Found groupaddress: {}", groupAddress);
            }
        }
        
        log.debug("Deleting temp files {}"+tmpFolder.getAbsolutePath());
        Path directory = Paths.get(tmpFolder.toURI());
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>(){

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

    }

    /**
     * Gets a list of projects. Typically the list has size=1
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

                for (Device device : project.getDeviceList()) {
                    Map<String, String> refMap = device.getRefMap();

                    String comObjInstanceRef = Utils.getKeyForValue(refMap, internalId);

                    if (comObjInstanceRef != null) {

                        // extract manufacturer id (which is folder name)
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

                        String dpt = cache.get(comObjInstanceRef);

                        if (dpt != null) {
                            log.debug("Found DPT: {}", dpt);

                            dpt = dpt.split(" ")[0];
                            String[] split = dpt.split("-");
                            if (split[0].equals("DPST")) {
                                int mainType = Integer.parseInt(split[1]);
                                int subType = Integer.parseInt(split[2]);
                                groupAddress.setDataType(mainType, subType);
                            } else if (split[0].equals("DPT")) {
                                int mainType = Integer.parseInt(split[1]);
                                int subType = 0;
                                groupAddress.setDataType(mainType, subType);
                            }
                            log.debug("Updated DPT: {}", groupAddress);
                        }

                    }
                }
            }

        }
    }

    private Map<String, String> createCache(File mFile) throws JDOMException, IOException {

        Map<String, String> cache = new HashMap<>();

        SAXBuilder builder = new SAXBuilder();

        Document document = (Document) builder.build(mFile);
        Element rootElement = document.getRootElement();
        Namespace ns = rootElement.getNamespace();

        Element applicationProgramsElement = rootElement.getChild("ManufacturerData", ns).getChild("Manufacturer", ns).getChild("ApplicationPrograms", ns);
        Element comObjectRefs = applicationProgramsElement.getChild("ApplicationProgram", ns).getChild("Static", ns).getChild("ComObjectRefs", ns);
        List<Element> children = comObjectRefs.getChildren("ComObjectRef", ns);
        for (Element comObjectRefElement : children) {
            String id = comObjectRefElement.getAttributeValue("Id");
            String dpt = comObjectRefElement.getAttributeValue("DatapointType");

            cache.put(id, dpt);
        }

        return cache;
    }

}
