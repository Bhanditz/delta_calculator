/**Created by Nada Eliba on January 2nd, 2018
* This class aims at comparing two versions of the same file
* to define the modified, inserted, and deleted lines
 * the two files are first sorted in-place,
 * then sdiff command is called to write the differences to a file,
 * suppressing the common lines between the two file versions.
 * Then further processing is performed on the output file;
 * in order to display the differences in a readable manner**/

package com.FileComparator;

import java.io.*;
import java.util.Scanner;

import static com.FileComparator.DWCAComparator.DWCA_Updates;


public class FileComparator{
   //public static File diffFile;
    public enum mode{
        delete,
       insert,
       reharvest
   }

    public static void main(String[] args) {}
    public static void compareFiles (String pathOne, String fileOne, String pathTwo, String fileTwo) throws IOException, InterruptedException
    {

        //Calls the Linux commands to be executed on the files
        FileComparator fileComparator = new FileComparator();

        if (fileOne.equals("")&&pathOne.equals("")) {
            // the file is being harvested for the first time
            File file2= new File(pathTwo+"/"+fileTwo);
            defineModificationType(file2, mode.insert,null);
            return;
        }
        if (fileTwo.equals("")&&pathTwo.equals(""))
        {
            File file1= new File(pathOne+"/"+fileOne);
            defineModificationType(file1, mode.delete,null);
            return;
        }
        // the file is being reharvested
        //set the command to sort the two file versions in-place
        fileComparator.executeCommand("sort "+pathOne+"/"+fileOne,pathOne+"/"+fileOne);
        fileComparator.executeCommand("sort "+pathTwo+"/"+fileTwo,pathTwo+"/"+fileTwo);
        int maxCountFile1 = getColumnCount(new File(pathOne+"/"+fileOne)),
                maxCountFile2=getColumnCount(new File(pathTwo+"/"+fileTwo)),
                columnCount=MaxValue(maxCountFile1,maxCountFile2);
                if(columnCount<130)
                    columnCount=130;
                if(columnCount<1000)
                    columnCount*=2;
        fileComparator.executeCommand("sdiff"+" "+pathOne+"/"+fileOne+" "+pathTwo+"/"+fileTwo+" -s"+" -H"+" -B"+" -b"+" -w"+columnCount,DWCA_Updates+"/"+fileOne);
        return;


        //get the maximum number of columns of both versions
         /** set the command to show the differences between the file versions
          sdiff shows the version differences side-by-side in one file
         -s for suppressing the common lines between both versions
         -H to grant high performance in case of large files
         -w to set the number of columns to be displayed in the output;
        the default width of sdiff is 130 columns, which may be too short in most cases
        so, the column count is being dynamically set according to each file**/
    }


    public static void executeCommand(String command) throws IOException, InterruptedException {
        Process process= Runtime.getRuntime().exec(command);
        process.waitFor();
        return;
    }
    public void executeCommand(String command, String file) throws IOException, InterruptedException {
        Process p= Runtime.getRuntime().exec(command);
        p.waitFor();
        File outputFile=new File(file);
        if(command.contains("sdiff"))
        {
            if(!outputFile.exists())
                outputFile.createNewFile();
            defineModificationType(outputFile,mode.reharvest,p.getInputStream());
            return;
        }
        if (command.contains("sort"))
        {
            BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedWriter bufferedWriter= new BufferedWriter(new FileWriter(file));
            String line = bufferedReader.readLine();
            while (line!= null) {
                bufferedWriter.write(line+"\n");
                line=bufferedReader.readLine();
            }
            commit(bufferedWriter);
            return;
        }
    }

private static int getColumnCount(File file) throws FileNotFoundException {
    int maxCount=0, lines=0, characters=0;
    Scanner fileScanner= new Scanner(new FileReader(file));
    while (fileScanner.hasNextLine()) {
        String line = fileScanner.nextLine();
        characters += line.length();
        if (maxCount < line.length()) {
            maxCount = line.length();
            lines++;
        }
    }
    return maxCount;
}

public static int MaxValue(int a, int b)
{
    int max =a;
    if (a<b)
        max=b;
    return max;
}

public static void defineModificationType(File outputFile, mode mode, InputStream inputStream) throws IOException {
    try {
        if(mode.equals(FileComparator.mode.reharvest))
        {
            BufferedReader sdiffBufferedReader= new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter sdiffBufferedWriter= new BufferedWriter(new FileWriter(outputFile));
            String sdiffLine = sdiffBufferedReader.readLine();
            String sdiffOutputLine="";
            while (sdiffLine!= null) {
                sdiffLine=sdiffLine.trim();
                if(sdiffLine.startsWith(">"))
                {
                    sdiffOutputLine="i: "+sdiffLine.substring(1);
                    sdiffBufferedWriter.write(sdiffOutputLine+"\n");
                }
                else if(sdiffLine.endsWith("<"))
                {
                    sdiffOutputLine="d: "+sdiffLine.substring(0,sdiffLine.length()-1);
                    sdiffBufferedWriter.write(sdiffOutputLine+"\n");
                }
                else
                {
                    sdiffOutputLine="e: "+sdiffLine.substring(sdiffLine.indexOf("|")+1);
                    sdiffBufferedWriter.write(sdiffOutputLine+"\n");
                }
                sdiffBufferedWriter.flush();
//                sdiffBufferedWriter.append(sdiffLine+"\n");
                System.out.println(sdiffLine);
                sdiffLine= sdiffBufferedReader.readLine();
            }
            sdiffBufferedWriter.write(sdiffOutputLine);
            commit(sdiffBufferedWriter);
//            sdiffBufferedReader.reset();
            sdiffBufferedReader.close();
            return;
        }
        File processedFile = new File(DWCA_Updates+"/"+outputFile.getName());
        if (!processedFile.exists())
            processedFile.createNewFile();
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(processedFile));
        String outputLine;
        FileReader fileReader=new FileReader(outputFile.getAbsoluteFile());
        BufferedReader bufferedReader=new BufferedReader(fileReader);
        String line=bufferedReader.readLine();
        while (line != null){
            //remove the white spaces at the beginning of the line, if any
        line=line.trim();
            if (mode.equals(FileComparator.mode.insert)) {
                // line is added in the new file or file is being harvested to the first time
                //print the new line with the prefix "i: "
                outputLine = "i: " + line.substring(1) + "\n";
                bufferedWriter.write(outputLine+"\n");
            } else if (mode.equals(FileComparator.mode.delete)) {
                // line removed from the first file
                // print the new line with the prefix "d: "
                outputLine = "d: " + line.substring(0, line.length() - 1) + "\n";
                bufferedWriter.write(outputLine+"\n");
            }
            //read the next line in the file
             line= bufferedReader.readLine();
        }
        commit(bufferedWriter);
        bufferedReader.close();
        return;
    }catch(Exception e){
        System.out.println(e);
    }
}
private static void commit(BufferedWriter bufferedWriter) throws IOException {
    bufferedWriter.flush();
    bufferedWriter.close();
    return;
}
}