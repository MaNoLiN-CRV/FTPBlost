package org.manolin.ftpblost.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.logging.LogManager;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.manolin.ftpblost.exceptions.FTPException;
import org.manolin.ftpblost.logs.LogsManager;

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
            LogsManager.logInfo("Connecting to " + server + ":" + port + " as " + user);
            ftpClient.connect(server, port);
            int replyCode = ftpClient.getReplyCode();
            LogsManager.logInfo("Server reply code: " + replyCode);
            
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                disconnect();
                throw new FTPException("FTP connection refused by the server: " + replyCode);
            }
            
            boolean loggedIn = ftpClient.login(user, password);
            if (!loggedIn) {
                disconnect();
                throw new FTPException("Could not log in to the FTP server.");
            }
            
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);  // Añadido
            
            LogsManager.logInfo("Working directory: " + ftpClient.printWorkingDirectory());
            LogsManager.logInfo("Connected successfully to FTP server");
            
        } catch (IOException e) {
            LogsManager.logError("Connection error: " + e.getMessage(), e);
            throw new FTPException("Error connecting to the FTP server: " + e.getMessage(), e);
        }
    }

    public void disconnect() throws FTPException {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
                LogsManager.logInfo("Disconnected from the FTP server.");
            } catch (IOException e) {
                throw new FTPException("Error disconnecting from the FTP server: " + e.getMessage(), e);
            }
        }
    }

    public void uploadFile(File localFile, String remotePath) throws FTPException {
        try (InputStream inputStream = new FileInputStream(localFile)) {
            LogsManager.logInfo("Uploading file to: " + remotePath);
            // Asegurar que el directorio existe
            String remoteDir = remotePath.substring(0, remotePath.lastIndexOf('/'));
            if (!remoteDir.isEmpty()) {
                makeDirectoryTree(remoteDir);
            }
            
            boolean done = ftpClient.storeFile(remotePath, inputStream);
            if (!done) {
                LogsManager.logError("Upload failed. Server reply: " + ftpClient.getReplyString(), null);
                throw new FTPException("The file could not be uploaded. " + ftpClient.getReplyString());
            }
        } catch (IOException e) {
            throw new FTPException("Error uploading file: " + e.getMessage(), e);
        }
    }

    public void downloadFile(String remotePath, File localFile) throws FTPException {
        try (OutputStream outputStream = new FileOutputStream(localFile)) {
            LogsManager.logInfo("Downloading file: " + remotePath + " to " + localFile.getAbsolutePath());
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
                LogsManager.logInfo("File deleted from the FTP server: " + remotePath);
            } else {
                LogsManager.logWarn("Could not delete the file from the FTP server: " + remotePath + ". Did it exist?");
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
                LogsManager.logInfo("Directory created on the FTP server: " + remotePath);
            } else {
                LogsManager.logWarn("Could not create the directory on the FTP server (it might already exist): " + remotePath);
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
    public boolean testFtpConnection() {
        try {
            connect();
            disconnect();
            return true;
        } catch (FTPException e) {
            LogsManager.logError("FTP connection test failed: " + e.getMessage(), e);
            return false;
        }
    }

    private void makeDirectoryTree(String dirPath) throws IOException {
        String[] dirs = dirPath.split("/");
        String currentDir = "";
        for (String dir : dirs) {
            if (!dir.isEmpty()) {
                currentDir = currentDir + "/" + dir;
                boolean exists = ftpClient.changeWorkingDirectory(currentDir);
                if (!exists) {
                    LogsManager.logInfo("Creating directory: " + currentDir);
                    ftpClient.makeDirectory(currentDir);
                }
            }
        }
        ftpClient.changeWorkingDirectory("/");
    }
}