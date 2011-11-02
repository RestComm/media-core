/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.testsuite.general.file;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mobicents.media.server.testsuite.general.AbstractTestCase;

/**
 *
 * @author baranowb
 */
public class FileUtils {

   public static final void serializeTestCase(AbstractTestCase testCase)
   {
    
 
     ObjectOutputStream oos = null;
        try {
            File f = new File(testCase.getTestDumpDirectory(), AbstractTestCase._CASE_FILE);
            //Should we care more here?
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
           oos = new ObjectOutputStream(fos);
            oos.writeObject(testCase);
        } catch (IOException ex) {
            ex.printStackTrace();
        }finally
        {
            if(oos!=null)
            {
                try {
                    oos.flush();
                    oos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
        }

    
   
   }
    
   public static final AbstractTestCase deserializeTestCase(File dumpDirectory) throws IllegalArgumentException
   {
  
 
     ObjectInputStream ois = null;
      File f = new File(dumpDirectory, AbstractTestCase._CASE_FILE);
        try {
           
            //Should we care more here?
            if (f.exists() && f.isFile()) {
              
            }else
            {
                throw new IllegalArgumentException("Wrong file indicator - it either does not exist or its not a file: "+f);
            }
         
            FileInputStream fis = new FileInputStream(f);
           ois = new ObjectInputStream(fis);
          Object o=  ois.readObject();
          AbstractTestCase atc= (AbstractTestCase) o;
          
          
          return atc;
        } catch (IOException ex) {
          throw new IllegalArgumentException("Faile to read.",ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("File: "+f+", indicates unknown test case class.",ex);
        } finally
        {
            if(ois!=null)
            {
                try {
                    ois.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
        }


   
   
   
   }
    
}
