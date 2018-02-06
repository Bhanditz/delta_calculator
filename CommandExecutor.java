package com.deltacalculator;

import java.io.*;

import static com.deltacalculator.DeltaCalculator.DWCADiff;

public class CommandExecutor {
    void removeDirectory(String directory) throws IOException, InterruptedException {
        System.out.println("Removing Directory: "+directory);
        Process removeDir = Runtime.getRuntime().exec("rm -r "+directory);
        removeDir.waitFor();
    }

    void compress(File dwcaDiff) throws IOException, InterruptedException {
        System.out.println("Compressing Archive: "+dwcaDiff.getName());
        Process compress = Runtime.getRuntime().exec("tar -czf "+dwcaDiff.getName()+".tar.gz"+" "+dwcaDiff.getName());
        compress.waitFor();
    }

    public File executeDiff (String file1, String file2) throws IOException, InterruptedException {
        ArchiveFileHandler archiveFileHandler = new ArchiveFileHandler();
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
        archiveFileHandler.commit(bufferedWriter);
        return diffFile;
    }

    public File executeSort (File inputFile) throws IOException, InterruptedException {
        System.out.println("Sorting File: "+inputFile.getPath());
        File outputFile = new File(inputFile.getPath());
        ExternalSort.sort(inputFile,outputFile);
        outputFile.renameTo(inputFile);
        return (outputFile);
    }
}
