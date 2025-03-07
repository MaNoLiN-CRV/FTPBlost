package org.manolin.ftpblost.controller;

import org.manolin.ftpblost.FileMonitor;
import org.manolin.ftpblost.exceptions.FTPException;
import org.manolin.ftpblost.managers.ConfigManager;
import org.manolin.ftpblost.managers.CryptoManager;
import org.manolin.ftpblost.managers.FTPManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

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
        System.out.println("FTPBlost Backup Manager - Thread Version");

        try {
            ftpManager.connect();
            ftpManager.makeDirectory(ftpRemoteBaseDir);

            Thread monitorThread = new Thread(() -> {
                try {
                    fileMonitor.startMonitoring();
                } catch (IOException | InterruptedException | FTPException e) {
                    System.err.println("Error monitoring files: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        ftpManager.disconnect(); 
                    } catch (FTPException e) {
                        System.err.println("Error disconnecting from FTP after monitoring failure: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            monitorThread.start();
            System.out.println("Monitoring started in background. Synchronizing changes from " + localDir + " to " + ConfigManager.FTP_SERVER + ":" + ftpRemoteBaseDir);

        } catch (FTPException e) {
            System.err.println("Initial FTP error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void downloadAndDecryptFile() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the path of the remote encrypted file to download (e.g. " + ftpRemoteBaseDir + "/mi_fichero.txt.encrypted): ");
        String remoteFileToDownload = scanner.nextLine();
        System.out.print("Enter the local path to save the decrypted file (e.g. descargado_descifrado.txt): ");
        String localFilePath = scanner.nextLine();
        File localFileDownload = new File(localFilePath);

        try {
            ftpManager.connect();
            ftpManager.downloadFile(remoteFileToDownload, localFileDownload);
            String cipherTextBase64 = Files.readString(localFileDownload.toPath());
            String decryptedText = CryptoManager.decryptText(cipherTextBase64, ConfigManager.AES_ENCRYPTION_KEY);
            System.out.println("Decrypted content of the file " + remoteFileToDownload + " saved in " + localFileDownload.getAbsolutePath() + ":\n" + decryptedText);
        } catch (Exception e) {
            System.err.println("Error downloading and decrypting file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                ftpManager.disconnect();
            } catch (FTPException e) {
                System.err.println("Error disconnecting from FTP after download: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n--- FTPBlost Backup Manager Menu ---");
            System.out.println("1. Start Synchronization in Background");
            System.out.println("2. Download and Decrypt File from FTP Server");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");
            String option = scanner.nextLine();

            switch (option) {
                case "1":
                    runBackupProcess();
                    System.out.println("Synchronization started in background.");
                    break;
                case "2":
                    downloadAndDecryptFile();
                    break;
                case "3":
                    running = false;
                    System.out.println("Exiting FTP Backup Manager.");
                    try {
                        ftpManager.disconnect();
                        System.out.println("Disconnected from the FTP server.");
                        System.exit(0);
                    } catch (FTPException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    System.out.println("Invalid option. Please select an option from the menu.");
            }
        }
        scanner.close();
    }
}