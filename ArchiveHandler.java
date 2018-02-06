package com.deltacalculator;

import org.apache.commons.io.FilenameUtils;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.deltacalculator.DeltaCalculator.DWCADiff;

public class ArchiveHandler {
    Archive openDwcAFolder(String path) throws IOException
    {
        Archive dwcArchive;
        System.out.println("Extracting Archive: " + path);
        File myArchiveFile = new File(path);
        File extractToFolder = new File(FilenameUtils.removeExtension(path) + ".out");
        dwcArchive = ArchiveFactory.openArchive(myArchiveFile, extractToFolder);
        String metaFiles[] = {"metadata.xml","meta.xml", "eml.xml"};
        int i;
        boolean metaFileExists = false;
        File metaFile = new File(dwcArchive.getLocation().getName() + "/" + metaFiles[0]);
        for (i = 0; i < metaFiles.length; i++) {
            metaFile = new File(dwcArchive.getLocation().getName() + "/" + metaFiles[i]);
            if (metaFile.exists()) {
                metaFileExists = true;
                break;
            }
        }
        if (metaFileExists == false)
            System.out.println("Meta File not Found!");
        else dwcArchive.setMetadataLocation(metaFile.getPath());
        return dwcArchive;
    }
    void filterContent(Archive version1, Archive version2) throws IOException, InterruptedException {

        ArrayList<ArchiveFile>
                archive1Content = setArchiveContentArrayList(version1),
                archive2Content = setArchiveContentArrayList(version2),
                archive1ContentTemp = setArchiveContentArrayList(version1),
                archive2ContentTemp = setArchiveContentArrayList(version2);
        int i;
        ArchiveFileHandler archiveFileHandler = new ArchiveFileHandler();

        boolean isAddedContent = archive2ContentTemp.removeAll(archive1Content),
                isDeletedContent = archive1ContentTemp.removeAll(archive2Content);

        if (isAddedContent){
            //mark new file as inserted
            for (i = 0; i < archive2ContentTemp.size(); i++) {
                File addedFile = new File(version2.getLocation().getName() + "/" + archive2ContentTemp.get(i));
                archiveFileHandler.readFromFileWriteToFile(addedFile, true, DWCADiff.getName());
                archive2Content.remove(archive2ContentTemp.get(i));
            }
        }

        if (isDeletedContent) {
            for (i = 0; i < archive1ContentTemp.size(); i++) {
                File deletedFile = new File(version1.getLocation().getName() + "/" + archive1ContentTemp.get(i));
                archiveFileHandler.readFromFileWriteToFile(deletedFile, false, DWCADiff.getName());
                archive1Content.remove(archive1ContentTemp.get(i));
            }
        }
        // if the two lists are identical, or reharvesting the rest of the two archives files
        archiveFileHandler.compareContent(version1,version2, archive2Content);
    }
    private ArrayList<ArchiveFile> setArchiveContentArrayList(Archive archive)
    {
        ArrayList<ArchiveFile> arrayList = new ArrayList<>();
        int i = 0;
        for(ArchiveFile archiveFile : archive.getExtensions()) {
            arrayList.add(i, archiveFile);
            i++;
        }
        arrayList.add(i, archive.getCore());
        i++;
        return arrayList;
    }

}
