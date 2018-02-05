package com.deltacalculator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;


public class DeltaCalculator {
static File DWCADiff=new File("DifferenceDynamicHierarchy_"+new Date().getTime());

public static void main(String[] args) throws IOException, InterruptedException {
        DeltaCalculator deltaCalculator = new DeltaCalculator();
        if(!DWCADiff.exists())
            DWCADiff.mkdir();
        File archiveVersion1 = new File("indianocean.tar.gz"),
             archiveVersion2 = new File("indianocean2.tar.gz");
        String archive1Path = archiveVersion1.getName(),
               archive2Path = archiveVersion2.getName();
        Archive dwca1 = deltaCalculator.openDwcAFolder(archive1Path),
                dwca2 = deltaCalculator.openDwcAFolder(archive2Path);
        System.out.println(dwca1.getMetadataLocation());
        System.out.println(dwca2.getMetadataLocation());
        File metaFile = new File (dwca2.getLocation().getName()+"/"+dwca2.getMetadataLocationFile().getName());
        if(!dwca2.getMetadataLocation().equals(null)) {
            System.out.println("Meta File Found - Sending File: " + metaFile.getPath());
            deltaCalculator.copyMetaFile(metaFile, DWCADiff.getName());
        }
        deltaCalculator.filterContent(dwca1,dwca2);
        deltaCalculator.compress(DWCADiff);
        deltaCalculator.removeDirectory(dwca1.getLocation().getName());
        deltaCalculator.removeDirectory(dwca2.getLocation().getName());
        deltaCalculator.removeDirectory(DWCADiff.getName());
    }


    private void removeDirectory(String directory) throws IOException, InterruptedException {
    System.out.println("Removing Directory: "+directory);
    Process removeDir = Runtime.getRuntime().exec("rm -r "+directory);
    removeDir.waitFor();
    }

    private void compress(File dwcaDiff) throws IOException, InterruptedException {
    System.out.println("Compressing Archive: "+dwcaDiff.getName());
    Process compress = Runtime.getRuntime().exec("tar -czf "+dwcaDiff.getName()+".tar.gz"+" "+dwcaDiff.getName());
    compress.waitFor();
    }

    private void filterContent(Archive version1, Archive version2) throws IOException, InterruptedException {

    ArrayList<ArchiveFile>
            archive1Content = new ArrayList<>(),
            archive2Content = new ArrayList<>(),
            archive1Names = new ArrayList<>(),
            archive2Names = new ArrayList<>(),
            archive1Temp = new ArrayList<>(),
            archive2Temp = new ArrayList<>();
    int i = 0;
//    int[] archive1FileHasHeader = new int [version1.getExtensions().size()+2],
//            archive2FileHasHeader = new int [version2.getExtensions().size()+2];

    for (ArchiveFile archiveFile : version1.getExtensions())
    {
        archive1Content.add(i, archiveFile);
        archive1Names.add(i, archive1Content.get(i));
        archive1Temp.add(i, archive1Content.get(i));
        System.out.println("Extension File number "+i+" is: "+archiveFile.getTitle());
        i++;
    }

    archive1Content.add(i, version1.getCore());
    archive1Names.add(i, archive1Content.get(i));
    archive1Temp.add(i, archive1Content.get(i));

    i=0;
    for (ArchiveFile archiveFile : version2.getExtensions())
    {
        archive2Content.add(i, archiveFile);
        archive2Names.add(i, archive2Content.get(i));
        archive2Temp.add(i, archive2Content.get(i));
        System.out.println("Extension File number "+i+" is: "+archiveFile.getTitle());
        i++;
    }

    archive2Content.add(i, version2.getCore());
    archive2Names.add(i, archive2Content.get(i));
    archive2Temp.add(i, archive2Content.get(i));



        boolean isAddedContent = archive2Temp.removeAll(archive1Names),
                isDeletedContent = archive1Temp.removeAll(archive2Names);

        if (isAddedContent){
            //mark new file as inserted
            for (i = 0; i < archive2Temp.size(); i++) {
                File addedFile = new File(version2.getLocation().getName() + "/" + archive2Temp.get(i));
                readWrite(addedFile, true, DWCADiff.getName());
                archive2Names.remove(archive2Temp.get(i));
            }
        }

        if (isDeletedContent) {
            for (i = 0; i < archive1Temp.size(); i++) {
                File deletedFile = new File(version1.getLocation().getName() + "/" + archive1Temp.get(i));
                readWrite(deletedFile, false, DWCADiff.getName());
                archive1Names.remove(archive1Temp.get(i));
            }
        }


        // if the two lists are identical, or reharvesting the rest of the two archives files
        compareContent(version1,version2, archive2Names);
    }

private void compareContent(Archive version1, Archive version2, ArrayList<ArchiveFile> content2) throws IOException, InterruptedException {

    for (int i = 0; i < content2.size(); i++) {
        File file1 = new File(version1.getLocation().getName() + "/" + content2.get(i).getTitle()),
             file2 = new File(version2.getLocation().getName() + "/" + content2.get(i).getTitle());

        System.out.println(file1.getPath()+", "+file2.getPath());
//        if ((content2.get(i).toString()).equals(version2.getMetadataLocationFile().getName())) {

//        }
//        else {
            String fileHeader = getHeader(file2);
            File
                    sortedFile1 = executeSort(file1),
                    sortedFile2 = executeSort(file2),
                    differenceFile = executeDiff(sortedFile1.getPath(), sortedFile2.getPath());

            if ((content2.get(i).getIgnoreHeaderLines())==1)
                addHeader(differenceFile, fileHeader);
        }
//}

}

private void copyMetaFile(File metaFile, String archivePathName) throws IOException {
        File targetMetaFile = new File(archivePathName+"/"+metaFile.getName());
        if (!targetMetaFile.exists())
        Files.copy(metaFile.toPath(),targetMetaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

public File executeDiff (String file1, String file2) throws IOException, InterruptedException {
    System.out.println("Comparing Files: "+file1+", "+file2);
    if(!DWCADiff.exists())
        DWCADiff.mkdir();
    File diffFile=new File(DWCADiff+"/"+file1.substring(file1.indexOf("/")+1,file1.length()));
    if (!diffFile.exists())
        diffFile.createNewFile();

    System.out.println("before sdiff directly");

    Process sDiff = Runtime.getRuntime().exec("sdiff "+file1+" "+ file2+ " -s"+" -H"+" -w"+" 2048");
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sDiff.getInputStream()));
    System.out.println("After bufferedreader ");
    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(diffFile));

    int i = 0;
    String line = bufferedReader.readLine();
    System.out.println("Before loop line is: " + line);
    while((line = bufferedReader.readLine())!=null){
        line = line.trim();
        System.out.println("Inside loop line is: " + line);
//        if (line.startsWith(">"))
//            bufferedWriter.write(lineInsert(line));
//        else if (line.endsWith("<"))
//            bufferedWriter.write(lineDelete(line).substring(0,lineDelete(line).length()-2)+"\n");
//        else
//            bufferedWriter.write(lineUpdate(line));
            bufferedWriter.write(line+"\n");
        i++;
    }

    System.out.println("i = " + i);
    bufferedReader.close();
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
    if(!StringUtils.isBlank(inputLine))
    {String insertedLine = "I: "+inputLine+"\n";
    return insertedLine;}
    else
        return ("");
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
private Archive openDwcAFolder(String path) throws IOException
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

}