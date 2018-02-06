package com.deltacalculator;

import org.apache.commons.lang3.StringUtils;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class ArchiveFileHandler {
    void copyMetaFile(File metaFile, String archivePathName) throws IOException {
        File targetMetaFile = new File(archivePathName+"/"+metaFile.getName());
        if (!targetMetaFile.exists())
            Files.copy(metaFile.toPath(),targetMetaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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

    public void readFromFileWriteToFile(File inputFile, boolean mode, String targetArchive) throws IOException {
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
    void compareContent(Archive version1, Archive version2, ArrayList<ArchiveFile> content2) throws IOException, InterruptedException {
        ArchiveFileHandler archiveFileHandler = new ArchiveFileHandler();
        CommandExecutor commandExecutor = new CommandExecutor();

        for (int i = 0; i < content2.size(); i++) {
            File file1 = new File(version1.getLocation().getName() + "/" + content2.get(i).getTitle()),
                    file2 = new File(version2.getLocation().getName() + "/" + content2.get(i).getTitle());

            System.out.println(file1.getPath()+", "+file2.getPath());
            String fileHeader = archiveFileHandler.getHeader(file2);
//            int index = content2.get(i).getCSVReader()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .getIndex();
//            System.out.println(index+": "+content2.get(i).getId().getTerm());
            File
                    sortedFile1 = commandExecutor.executeSort(file1),
                    sortedFile2 = commandExecutor.executeSort(file2),
                    differenceFile = commandExecutor.executeDiff(sortedFile1.getPath(), sortedFile2.getPath());

            if ((content2.get(i).getIgnoreHeaderLines())==1)
                archiveFileHandler.addHeader(differenceFile, fileHeader);
        }
//}

    }

}
