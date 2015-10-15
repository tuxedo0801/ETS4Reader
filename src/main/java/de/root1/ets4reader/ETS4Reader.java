/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.ets4reader;

import java.io.File;
import java.io.IOException;
import org.jdom2.JDOMException;

/**
 *
 * @author achristian
 */
public class ETS4Reader {
    
    public static void main(String[] args) throws IOException, JDOMException {
        KnxProjReader knxProjReader = new KnxProjReader(new File(args[0]));
    }
    
}
