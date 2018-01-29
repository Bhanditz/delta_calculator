package com.deltacalculator;

import org.apache.commons.io.FilenameUtils;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;

public class DeltaCalculator {
    static File DWCADiff=new File("DifferenceDynamicHierarchy");

    public static void main(String[] args) throws IOException, InterruptedException {
        DeltaCalculator deltaCalculator = new DeltaCalculator();
        File version1 = new File("4.tar.gz"),
             version2 = new File("8.tar.gz");
//
//        deltaCalculator.execDecompress(version1.getName());
//        deltaCalculator.execDecompress(version2.getName());

//        File archive1 = new File(version1.getName().substring(0, version1.getName().indexOf('.'))),
//             archive2 = new File(version2.getName().substring(0, version2.getName().indexOf('.')));

//        String archive1Path = archive1.getName(),
//               archive2Path = archive2.getName();

        String archive1Path = version1.getName(),
                archive2Path = version2.getName();

        Archive dwca1 = deltaCalculator.openDwcAFolder(archive1Path) ,
                dwca2 = deltaCalculator.openDwcAFolder(archive2Path);

//        Set<ArchiveFile> archive1Files = dwca1.getExtensions(),
//                         archive2Files = dwca2.getExtensions();
//for (ArchiveFile archiveFile:dwca1.getExtensions())
//{
//    archive1Files.add(archiveFile);
////    dwca1.setExtensions(archiveFile);
//}
//System.out.println(archive1Files.size()+", "+archive2Files.size());
        ArrayList<String>
                archive1Content = new ArrayList<>(),
                archive2Content = new ArrayList<>(),
                archive1Names = new ArrayList<>(),
                archive2Names = new ArrayList<>(),
                archive1Temp = new ArrayList<>(),
                archive2Temp = new ArrayList<>();
        int i = 0;
//            archive1Size = archive1Files.size(),
//            archive2Size = archive2Files.size();

        int[] archive1FileHasHeader = new int [dwca1.getExtensions().size()],
              archive2FileHasHeader = new int [dwca2.getExtensions().size()];
        for (ArchiveFile archiveFile : dwca1.getExtensions())
        {
            archive1Content.add(i, archiveFile.getTitle());
            archive1Names.add(i, archive1Content.get(i).toString());
            archive1Temp.add(i, archive1Content.get(i).toString());
            archive1FileHasHeader[i] = archiveFile.getIgnoreHeaderLines();
            i++;
        }
        i=0;
        for (ArchiveFile archiveFile : dwca2.getExtensions())
        {
            archive2Content.add(i, archiveFile.getTitle());
            archive2Names.add(i, archive2Content.get(i).toString());
            archive2Temp.add(i, archive2Content.get(i).toString());
            archive2FileHasHeader[i] = archiveFile.getIgnoreHeaderLines();
            i++;
        }
        if (!DWCADiff.exists())
            DWCADiff.mkdir();

        Collections.sort(archive1Names);
        Collections.sort(archive2Names);
        Collections.sort(archive1Temp);
        Collections.sort(archive1Temp);

        boolean isAddedContent = archive2Temp.removeAll(archive1Names),
                isDeletedContent = archive1Temp.removeAll(archive2Names);

        if (isAddedContent){
            //mark new file as inserted
            for (i = 0; i < archive2Temp.size(); i++) {
                File addedFile = new File(archive2Path + "/" + archive2Temp.get(i));
                deltaCalculator.readWrite(addedFile, true, DWCADiff.getName());
                archive2Names.remove(archive2Temp.get(i));
            }
    }

        if (isDeletedContent) {
            for (i = 0; i < archive1Temp.size(); i++) {
                File deletedFile = new File(archive1Path + "/" + archive1Temp.get(i));
                deltaCalculator.readWrite(deletedFile, false, DWCADiff.getName());
                archive1Names.remove(archive1Temp.get(i));
            }
        }

        // if the two lists are identical, or reharvesting the rest of the two archives files
        for (i = 0; i < archive2Names.size(); i++) {
            File file1 = new File(dwca1.getLocation().getName() + "/" + archive1Names.get(i)),
                    file2 = new File(dwca2.getLocation().getName() + "/" + archive2Names.get(i));
            System.out.println(file1.getPath()+", "+file2.getPath());
            if (archive2Names.get(i).contains("meta")) {
                System.out.println("Meta File Found - Sending File: "+archive2Names.get(i));
                deltaCalculator.setMetaFile(file2, DWCADiff.getName());
            }
            else {

                System.out.println(archive2Names.get(i));
                String fileHeader = deltaCalculator.getHeader(file2);
                File sortedFile1 = deltaCalculator.executeSort(file1),
                        sortedFile2 = deltaCalculator.executeSort(file2),
                        differenceFile = deltaCalculator.executeDiff(sortedFile1.getPath(), sortedFile2.getPath());
                if (archive2FileHasHeader[i]==1)
                deltaCalculator.addHeader(differenceFile, fileHeader);
        }}
        System.gc();
    }

    private void setMetaFile(File metaFile, String archivePathName) throws IOException {
        File targetMetaFile = new File(archivePathName+"/"+metaFile.getName());
        Files.copy(metaFile.toPath(),targetMetaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public File executeDiff (String file1, String file2) throws IOException, InterruptedException {
    Process sDiff = Runtime.getRuntime().exec("sdiff "+file1+" "+ file2+ " -s"+" -H"+" -w"+" 2048");
    sDiff.waitFor();
    System.out.println("Comparing Files: "+file1+", "+file2);
    if(!DWCADiff.exists())
    DWCADiff.mkdir();
        File diffFile=new File(DWCADiff+"/"+file1.substring(file1.indexOf("/")+1,file1.length()));
    if (!diffFile.exists())
        diffFile.createNewFile();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sDiff.getInputStream()));
    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(diffFile));
    String line = bufferedReader.readLine();
    while(line!=null)
    {
        line = line.trim();
        if (line.startsWith(">"))
            bufferedWriter.write(lineInsert(line));
        else if (line.endsWith("<"))
            bufferedWriter.write(lineDelete(line).substring(0,lineDelete(line).length()-2)+"\n");
        else
            bufferedWriter.write(lineUpdate(line));
        line=bufferedReader.readLine();
    }
    commit(bufferedWriter);
    return diffFile;
}

public File executeSort (File inputFile) throws IOException, InterruptedException {
    System.out.println("Sorting File: "+inputFile.getPath());
    File outputFile = new File(inputFile.getPath());
    ExternalSort.sort(inputFile,outputFile);
    outputFile.renameTo(inputFile);
    return (outputFile);
}
public String getHeader(File file) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new FileReader(file.getPath()));
    String fileHeader = bufferedReader.readLine();
    return (fileHeader);
}

public File addHeader(File file, String header) throws IOException {
    File temp = new File(file.getPath()+"temp");
    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp,true));
    bufferedWriter.write(header+"\n");
    String line = bufferedReader.readLine();
    while(line!=null)
    {
        bufferedWriter.write(line+"\n");
        line = bufferedReader.readLine();
    }
    commit(bufferedWriter);
    temp.renameTo(file);
    return (temp);
}

public void execDecompress(String archiveName) throws IOException, InterruptedException {
    Process decompress= Runtime.getRuntime().exec("tar "+"-xvzf "+archiveName);
    decompress.waitFor();
    System.out.println("Decompressing Archive: "+archiveName);
}

public void commit(BufferedWriter bufferedWriter) throws IOException {
    bufferedWriter.flush();
    bufferedWriter.close();
}

public void readWrite(File inputFile, boolean mode, String targetArchive) throws IOException {
    BufferedReader readInput = new BufferedReader(new FileReader(inputFile));
    File outputFile = new File(targetArchive+"/"+inputFile.getName());
    if (!outputFile.exists())
        outputFile.createNewFile();
    BufferedWriter writeOutput = new BufferedWriter(new FileWriter(outputFile));
    String inputLine = readInput.readLine();
    while (inputLine!=null)
    {
        if(mode)
        writeOutput.write(lineInsert(inputLine));
        else
        writeOutput.write(lineDelete(inputLine));
    inputLine = readInput.readLine();
}
    commit(writeOutput);
    }

public String lineInsert(String inputLine)
{
    inputLine = inputLine.substring(1,inputLine.length());
    inputLine = inputLine.trim();
    String insertedLine = "I: "+inputLine+"\n";
    return insertedLine;
}
public String lineDelete(String inputLine)
    {
        String deletedLine = "D: "+inputLine.substring(0,inputLine.length())+"\n";
        return deletedLine;
    }
public String lineUpdate(String inputLine)
    {
        String updatedLine = "U: "+inputLine.substring(inputLine.indexOf('|')+1,inputLine.length())+"\n";
        return updatedLine;
    }
    private Archive openDwcAFolder(String path){
        Archive dwcArchive;
        try {
            File myArchiveFile = new File(path);
            File extractToFolder = new File(FilenameUtils.removeExtension(path).substring(0,FilenameUtils.removeExtension(path).indexOf(".")));
            dwcArchive = ArchiveFactory.openArchive(myArchiveFile, extractToFolder);
        } catch (Exception e) {
            System.out.println("Failed to parse the Darwin core archive " + e.getMessage());
//            logger.debug("Failed to Parse the Darwin Core Archive"+e.getMessage());
            return null;
        }
        return dwcArchive;
    }
}