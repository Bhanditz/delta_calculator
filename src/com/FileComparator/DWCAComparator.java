package com.FileComparator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static com.FileComparator.FileComparator.compareFiles;
import static com.FileComparator.FileComparator.executeCommand;


/** Created by Nada Eliba on January 3rd, 2018
 * This class compares two versions of a given Darwin Core Archive
 * The two versions are first extracted
 * Then each file of the first version is compared to the corresponding file of the second version**/
public class DWCAComparator {
public static File DWCA_Updates= new File("DWCA_Updates");

    public static void main (String args[]) throws IOException, InterruptedException {
        DWCAComparator comparator=new DWCAComparator();
        if (!DWCA_Updates.exists())
            DWCA_Updates.mkdir();
        String archive1="4.tar.gz",
                archive2="8.tar.gz",
                folder1=archive1.substring(0,archive1.indexOf('.')),
                folder2=archive2.substring(0,archive2.indexOf('.'));
        executeCommand("tar xzf "+archive1);
        executeCommand("tar xzf "+archive2);
        comparator.dwcaCompare(folder1,folder2);
        executeCommand("tar -czvf DWCA_Updates.tar.gz DWCA_Updates");
        executeCommand("rm -r "+archive1.substring(0,archive1.indexOf(".")));
        executeCommand("rm -r "+archive2.substring(0,archive2.indexOf(".")));
        executeCommand("rm -r "+DWCA_Updates);
        return;
    }

    private int dwcaCompare(String path1,String path2) throws IOException, InterruptedException {
if (path1==null)
    //There is only one version of the DWCA
    return 2;
//iterate on both versions of DWCA
        File version1=new File(path1),
                version2= new File(path2);
        File[] version1Content= version1.listFiles(),
        version2Content=version2.listFiles();
        ArrayList<String> version1Names = new ArrayList<String>(),
                version2Names=new ArrayList<String>(),
                version1Temp=new ArrayList<String>(),
                version2Temp= new ArrayList<String>();
        int i;
        //get only the file names, independent of the file path
        for(i=0;i<version1Content.length;i++) {
            version1Names.add(i, version1Content[i].getName());
            version1Temp.add(i, version1Content[i].getName());
        }
        for(i=0;i<version2Content.length;i++) {
            version2Names.add(i, version2Content[i].getName());
            version2Temp.add(i, version2Content[i].getName());
        }

        Collections.sort(version1Names);
        Collections.sort(version2Names);
        /**find the difference between the two lists
        check for changed file list between both versions**/
        boolean isAddedContent=version2Temp.removeAll(version1Names),
        isDeletedContent=version1Temp.removeAll(version2Names);
        if(isAddedContent)
        {//mark new file as inserted
            for(i=0;i<version2Temp.size();i++) {
                compareFiles("","",path2, version2Temp.get(i));
                version2Names.remove(version2Temp.get(i));
            }
             }
        if(isDeletedContent)
        {
            //mark file as deleted
            for(i=0;i<version1Temp.size();i++) {
                compareFiles(path1, version1Temp.get(i),"","");
                version1Names.remove(version1Temp.get(i));
            }
        }
        // if the two lists are identical, or reharvesting the rest of the two archives files
       for(i=0;i<version1Names.size();i++)
            compareFiles(path1, version1Names.get(i), path2, version2Names.get(i));
        return 1;
    }
}