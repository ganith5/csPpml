package com.shutterfly.cp;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.junit.Assert;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by psachidananda on 8/27/14.
 */
public class FileCopyPaste {

    private static String colorSurePpmlPath = "/Users/psachidananda/lab-perforce/depot/LabService/CQVAssets/Indigo-CS-PPML/";
    private static String username = "calib";
    private static String password = "calib";
    private static final String sharedHolder = "ripoutput/ColorSure";
    private static String remotePath = "smb://%s/"+sharedHolder;
    private static String domain = "localhost";
    private static String hostNameFileName;

    final static DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
        public boolean accept(Path file) throws IOException {
            return (file.getFileName().toString().endsWith("ppml") ||
                    file.getFileName().toString().endsWith("pdf"));
        }
    };

    public static void main(String[] args) {
        readHnUserPwd(args);
        String[] hostNames = readCSV();
        try{
            for(String hostName : hostNames){
                System.out.println(hostName);
//                if(doesServerExist(hostName.trim())){
//                    System.out.println(hostName + " exists");
//                    copyFiles(String.format(remotePath, hostName));
//                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readHnUserPwd(String[] inputArgs){
        for (int i=0; i<inputArgs.length; i++){
            if(inputArgs[i].equals("-colorSurePpmlPath")){
                colorSurePpmlPath = inputArgs[++i];
            } else if(inputArgs[i].equals("-domain")) {
                domain = inputArgs[++i];
            } else if(inputArgs[i].equals("-username")) {
                username = inputArgs[++i];
            } else if(inputArgs[i].equals("-password")) {
                password = inputArgs[++i];
            } else if(inputArgs[i].equals("-hostNamesFile")) {
                hostNameFileName = inputArgs[++i];
            }

        }
    }

    private static String[] readCSV() {
        Assert.assertNotNull("Hostname file not set", hostNameFileName);
        Path inputFile = Paths.get(hostNameFileName);
        try {
            byte[] hostNamesAsBytes = Files.readAllBytes(inputFile);
            String hostNames = new String(hostNamesAsBytes);
            String[] hostName = hostNames.split(",");
            return hostName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void copyFiles(String remoteBaseFilePath) {
        Path localColorSurePpml = Paths.get(colorSurePpmlPath);
        NtlmPasswordAuthentication ntlmPasswordAuthentication = new NtlmPasswordAuthentication(domain, username, password);
        int count = 0;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(localColorSurePpml, filter)){
            for (Path filePath : directoryStream) {
                String ppmlFileName = filePath.getFileName().toString();
                byte[] ppmlBytes = Files.readAllBytes(filePath);
                String remoteFileName = String.format("%s/%s", remoteBaseFilePath, ppmlFileName);
                SmbFile smbFile = new SmbFile(remoteFileName, ntlmPasswordAuthentication);
                if(smbFile.exists()){
                    smbFile.delete();
                }
                SmbFileOutputStream smbFileOutputStream = new SmbFileOutputStream(smbFile);
                smbFileOutputStream.write(ppmlBytes);
                count++;
            }
            System.out.println("Number of files copied = " + count);
        } catch (IOException e) {
            e.printStackTrace();
            count--;
        }
    }

    private static boolean doesServerExist(String hostName) {
        String server = String.format(remotePath,hostName);

        NtlmPasswordAuthentication ntlmPasswordAuthentication = new NtlmPasswordAuthentication(domain, username, password);

        try {
            SmbFile smbFile = new SmbFile(server, ntlmPasswordAuthentication);
            smbFile.connect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        return true;
    }


}
