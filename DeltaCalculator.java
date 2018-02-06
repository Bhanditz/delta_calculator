package com.deltacalculator;


import org.gbif.dwca.io.Archive;
import java.io.*;
import java.util.Date;


public class DeltaCalculator {
static File DWCADiff=new File("DifferenceDynamicHierarchy_"+new Date().getTime());

public static void main(String[] args) throws IOException, InterruptedException {
        ArchiveHandler archiveHandler = new ArchiveHandler();
        CommandExecutor commandExecutor = new CommandExecutor();
        if(!DWCADiff.exists())
            DWCADiff.mkdir();
        File archiveVersion1 = new File("indianocean.tar.gz"),
             archiveVersion2 = new File("indianocean2.tar.gz");
        String archive1Path = archiveVersion1.getName(),
               archive2Path = archiveVersion2.getName();
        Archive dwca1 = archiveHandler.openDwcAFolder(archive1Path),
                dwca2 = archiveHandler.openDwcAFolder(archive2Path);
        System.out.println(dwca1.getMetadataLocation());
        System.out.println(dwca2.getMetadataLocation());
        File metaFile = new File (dwca2.getLocation().getName()+"/"+dwca2.getMetadataLocationFile().getName());
        if(!dwca2.getMetadataLocation().equals(null)) {
            System.out.println("Meta File Found - Sending File: " + metaFile.getPath());
            ArchiveFileHandler archiveFileHandler = new ArchiveFileHandler();
            archiveFileHandler.copyMetaFile(metaFile, DWCADiff.getName());
        }
        archiveHandler.filterContent(dwca1,dwca2);
        commandExecutor.compress(DWCADiff);
        commandExecutor.removeDirectory(dwca1.getLocation().getName());
        commandExecutor.removeDirectory(dwca2.getLocation().getName());
        commandExecutor.removeDirectory(DWCADiff.getName());
    }

}