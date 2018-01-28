package com.deltacalculator;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DeltaCalculator {
    static File DWCADiff=new File("DifferenceDynamicHierarchy");

    public static void main(String[] args) throws IOException, InterruptedException {
        DeltaCalculator deltaCalculator = new DeltaCalculator();
        File version1 = new File("eoldynamichierarchyv1revised.tar.gz"),
             version2 = new File("eoldynamichierarchyv2revised.tar.gz");

        deltaCalculator.execDecompress(version1.getName());
        deltaCalculator.execDecompress(version2.getName());

        File archive1 = new File(version1.getName().substring(0, version1.getName().indexOf('.'))),
             archive2 = new File(version2.getName().substring(0, version2.getName().indexOf('.')));

        String archive1Path = archive1.getName(),
               archive2Path = archive2.getName();


        ArrayList<String> archive1Content = new ArrayList<>(Arrays.asList(archive1.list())),
                archive2Content = new ArrayList<>(Arrays.asList(archive2.list())),
                archive1Names = new ArrayList<>(),
                archive2Names = new ArrayList<>(),
                archive1Temp = new ArrayList<>(),
                archive2Temp = new ArrayList<>();

        if (!DWCADiff.exists())
            DWCADiff.mkdir();
        int i;
        for (i = 0; i < archive1Content.size(); i++) {
            archive1Names.add(i, archive1Content.get(i));
            archive1Temp.add(i, archive1Content.get(i));
        }
        for (i = 0; i < archive2Content.size(); i++) {
            archive2Names.add(i, archive2Content.get(i));
            archive2Temp.add(i, archive2Content.get(i));
        }

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
        for (i = 0; i < archive1Names.size(); i++) {
            File file1 = new File(archive1Path + "/" + archive1Names.get(i)),
                    file2 = new File(archive2Path + "/" + archive1Names.get(i));
            if (file2.getName().contains("meta")) {
                System.out.println("Meta File Found - Sending File: "+file2.getName());
                deltaCalculator.setMetaFile(file2, DWCADiff.getName());
            }
            else {
                String fileHeader = deltaCalculator.getHeader(file2);
                File sortedFile1 = deltaCalculator.executeSort(file1),
                        sortedFile2 = deltaCalculator.executeSort(file2),
                        differenceFile = deltaCalculator.executeDiff(sortedFile1.getPath(), sortedFile2.getPath());
                deltaCalculator.addHeader(differenceFile, fileHeader);
            }
        }
        System.gc();
    }

    private void setMetaFile(File metaFile, String archivePathName) throws IOException {
        File targetMetaFile = new File(archivePathName+"/"+metaFile.getName());
        Files.copy(metaFile.toPath(), targetMetaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
}