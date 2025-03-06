package org.manolin.ftpblost.managers;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.manolin.ftpblost.exceptions.FTPException;

import java.io.*;

public class FTPManager {

    private final FTPClient ftpClient;
    private final String server;
    private final int port;
    private final String user;
    private final String password;

    public FTPManager(String server, int port, String user, String password) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;
        this.ftpClient = new FTPClient();
    }

    public void connect() throws FTPException {
        try {
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                disconnect();
                throw new FTPException("FTP connection refused by the server: " + replyCode);
            }
            boolean loggedIn = ftpClient.login(user, password);
            if (!loggedIn) {
                disconnect();
                throw new FTPException("Could not log in to the FTP server.");
            }
            ftpClient.enterLocalPassiveMode(); // <-- Passive mode
            System.out.println("Connected to the FTP server: " + server);
        } catch (IOException e) {
            throw new FTPException("Error connecting to the FTP server: " + e.getMessage(), e);
        }
    }

    public void disconnect() throws FTPException {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
                System.out.println("Disconnected from the FTP server.");
            } catch (IOException e) {
                throw new FTPException("Error disconnecting from the FTP server: " + e.getMessage(), e);
            }
        }
    }

    public void uploadFile(File localFile, String remotePath) throws FTPException {
        try (InputStream inputStream = new FileInputStream(localFile)) {
            System.out.println("Uploading file: " + localFile.getAbsolutePath() + " to " + remotePath);
            boolean done = ftpClient.storeFile(remotePath, inputStream);
            if (!done) {
                throw new FTPException("The file could not be uploaded to the FTP server.");
            }
        } catch (IOException e) {
            throw new FTPException("Error uploading the file " + localFile.getAbsolutePath() + " to the FTP server: " + e.getMessage(), e);
        }
    }

    public void downloadFile(String remotePath, File localFile) throws FTPException {
        try (OutputStream outputStream = new FileOutputStream(localFile)) {
            System.out.println("Downloading file: " + remotePath + " to " + localFile.getAbsolutePath());
            boolean done = ftpClient.retrieveFile(remotePath, outputStream);
            if (!done) {
                throw new FTPException("The file could not be downloaded from the FTP server.");
            }
        } catch (IOException e) {
            throw new FTPException("Error downloading the file " + remotePath + " from the FTP server: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String remotePath) throws FTPException {
        try {
            boolean deleted = ftpClient.deleteFile(remotePath);
            if (deleted) {
                System.out.println("File deleted from the FTP server: " + remotePath);
            } else {
                System.out.println("Could not delete the file from the FTP server: " + remotePath + ". Did it exist?");
            }
        } catch (IOException e) {
            throw new FTPException("Error deleting the file " + remotePath + " from the FTP server: " + e.getMessage(), e);
        }
    }

    public boolean fileExists(String remotePath) throws FTPException {
        try {
            return ftpClient.listNames(remotePath) != null && ftpClient.listNames(remotePath).length > 0;
        } catch (IOException e) {
            throw new FTPException("Error checking if the file exists on the FTP server: " + e.getMessage(), e);
        }
    }

    public void makeDirectory(String remotePath) throws FTPException {
        try {
            boolean created = ftpClient.makeDirectory(remotePath);
            if (created) {
                System.out.println("Directory created on the FTP server: " + remotePath);
            } else {
                System.out.println("Could not create the directory on the FTP server (it might already exist): " + remotePath);
            }
        } catch (IOException e) {
            throw new FTPException("Error creating the directory " + remotePath + " on the FTP server: " + e.getMessage(), e);
        }
    }

    public String[] listFiles(String remotePath) throws FTPException {
        try {
            return ftpClient.listNames(remotePath);
        } catch (IOException e) {
            throw new FTPException("Error listing files on the FTP server: " + e.getMessage(), e);
        }
    }
}