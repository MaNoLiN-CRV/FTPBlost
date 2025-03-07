package org.manolin.ftpblost.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

import org.logs.LogsManager;
import org.manolin.ftpblost.FileMonitor;
import org.manolin.ftpblost.exceptions.FTPException;
import org.manolin.ftpblost.managers.ConfigManager;
import org.manolin.ftpblost.managers.CryptoManager;
import org.manolin.ftpblost.managers.FTPManager;

public class BackupController {

    private final FileMonitor fileMonitor;
    private final FTPManager ftpManager;
    private final String localDir;
    private final String ftpRemoteBaseDir;

    public BackupController() {
        this.localDir = ConfigManager.LOCAL_DIRECTORY_TO_WATCH;
        String ftpServer = ConfigManager.FTP_SERVER;
        int ftpPort = ConfigManager.FTP_PORT;
        String ftpUser = ConfigManager.FTP_USER;
        String ftpPassword = ConfigManager.FTP_PASSWORD;
        this.ftpRemoteBaseDir = ConfigManager.FTP_REMOTE_BASE_DIRECTORY;

        this.ftpManager = new FTPManager(ftpServer, ftpPort, ftpUser, ftpPassword);
        this.fileMonitor = new FileMonitor(localDir, ftpManager, ftpRemoteBaseDir);
    }

    public void runBackupProcess() {
        LogsManager.logInfo("FTPBlost Backup Manager - Thread Version");

        try {
            ftpManager.connect();
            ftpManager.makeDirectory(ftpRemoteBaseDir);

            Thread monitorThread = new Thread(() -> {
                try {
                    fileMonitor.startMonitoring();
                } catch (IOException | InterruptedException | FTPException e) {
                    LogsManager.logError("Error monitoring files: " + e.getMessage(), e);
                } finally {
                    try {
                        ftpManager.disconnect(); 
                    } catch (FTPException e) {
                        LogsManager.logError("Error disconnecting from FTP after monitoring failure: " + e.getMessage(), e);
                    }
                }
            });
            monitorThread.start();
            LogsManager.logInfo("Monitoring started in background. Synchronizing changes from " + localDir + " to " + ConfigManager.FTP_SERVER + ":" + ftpRemoteBaseDir);

        } catch (FTPException e) {
            LogsManager.logError("Initial FTP error: " + e.getMessage(), e);
        }
    }

    public void downloadAndDecryptFile() {
        try (Scanner scanner = new Scanner(System.in)) {
            LogsManager.logInfo("Enter the path of the remote encrypted file to download (e.g. " + ftpRemoteBaseDir + "/mi_fichero.txt.encrypted): ");
            String remoteFileToDownload = scanner.nextLine();
            LogsManager.logInfo("Enter the local path to save the decrypted file (e.g. descargado_descifrado.txt): ");
            String localFilePath = scanner.nextLine();
            File localFileDownload = new File(localFilePath);

            try {
                ftpManager.connect();
                ftpManager.downloadFile(remoteFileToDownload, localFileDownload);
                String cipherTextBase64 = Files.readString(localFileDownload.toPath());
                String decryptedText = CryptoManager.decryptText(cipherTextBase64, ConfigManager.AES_ENCRYPTION_KEY);
                LogsManager.logInfo("Decrypted content of the file " + remoteFileToDownload + " saved in " + localFileDownload.getAbsolutePath());
                LogsManager.logDebug("Decrypted content:\n" + decryptedText);
            } catch (Exception e) {
                LogsManager.logError("Error downloading and decrypting file: " + e.getMessage(), e);
            } finally {
                try {
                    ftpManager.disconnect();
                } catch (FTPException e) {
                    LogsManager.logError("Error disconnecting from FTP after download: " + e.getMessage(), e);
                }
            }
        }
    }

    public void showMenu() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            
            while (running) {
                LogsManager.logInfo("\n--- FTPBlost Backup Manager Menu ---");
                LogsManager.logInfo("1. Start Synchronization in Background");
                LogsManager.logInfo("2. Download and Decrypt File from FTP Server");
                LogsManager.logInfo("3. Exit");
                LogsManager.logInfo("Select an option: ");
                String option = scanner.nextLine();
                
                switch (option) {
                    case "1" -> {
                        runBackupProcess();
                        LogsManager.logInfo("Synchronization started in background.");
                    }
                    case "2" -> downloadAndDecryptFile();
                    case "3" -> {
                        running = false;
                        LogsManager.logInfo("Exiting FTP Backup Manager.");
                        try {
                            ftpManager.disconnect();
                            LogsManager.logInfo("Disconnected from the FTP server.");
                            System.exit(0);
                        } catch (FTPException e) {
                            LogsManager.logError("Error during shutdown: " + e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                    default -> LogsManager.logWarn("Invalid option. Please select an option from the menu.");
                }
            }
        }
    }
}