package org.manolin.ftpblost;

import org.manolin.ftpblost.exceptions.FTPException;
import org.manolin.ftpblost.managers.ConfigManager;
import org.manolin.ftpblost.managers.CryptoManager;
import org.manolin.ftpblost.managers.FTPManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileMonitor {

    private final Path directoryToWatch;
    private final FTPManager ftpManager;
    private final String remoteBasePath;
    private final ExecutorService executorService = Executors.newFixedThreadPool(ConfigManager.THREAD_POOL_SIZE);

    private final Map<Path, Long> lastModifiedTimes = new HashMap<>();

    public FileMonitor(String directoryToWatch, FTPManager ftpManager, String remoteBasePath) {
        this.directoryToWatch = Paths.get(directoryToWatch);
        this.ftpManager = ftpManager;
        this.remoteBasePath = remoteBasePath;
    }

    public void startMonitoring() throws IOException, InterruptedException, FTPException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        directoryToWatch.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        System.out.println("Monitoring local directory: " + directoryToWatch);
        Files.walkFileTree(directoryToWatch, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) {
                    lastModifiedTimes.put(file.toAbsolutePath(), Files.getLastModifiedTime(file).toMillis());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        AtomicBoolean poll = new AtomicBoolean(true);
        while (poll.get()) {
            WatchKey key = watchService.take();
            if (key != null) {
                executorService.submit(() -> {
                    try {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path filename = ev.context();
                            Path child = directoryToWatch.resolve(filename);

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                System.out.println("File created: " + filename);
                                syncFileToFTP(child, remoteBasePath + "/" + filename);

                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                long currentLastModified = Files.getLastModifiedTime(child).toMillis();

                                if (!lastModifiedTimes.containsKey(child.toAbsolutePath()) ||
                                        lastModifiedTimes.get(child.toAbsolutePath()) < currentLastModified) {

                                    System.out.println("File modified: " + filename);
                                    syncFileToFTP(child, remoteBasePath + "/" + filename);
                                    lastModifiedTimes.put(child.toAbsolutePath(), currentLastModified);

                                }

                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("File deleted: " + filename);
                                deleteFileFromFTP(remoteBasePath + "/" + filename);
                                lastModifiedTimes.remove(child.toAbsolutePath());
                            }
                        }
                        poll.set(key.reset());
                    } catch (IOException e) {
                        System.out.println("Error at FileMonitor: " + e.getMessage());
                        e.printStackTrace();
                    }

                });


            } else {
                poll.set(false);
            }
        }
        watchService.close();
    }

    private void syncFileToFTP(Path localFilePath, String remoteFilePath) {
        File localFile = localFilePath.toFile();
        if (!localFile.isFile()) {
            System.out.println("Ignoring non-file: " + localFilePath);
            return;
        }
        String encryptedText = null;
        boolean isTextFile = isTextFile(localFilePath);
        try {
            String remoteFullPath = remoteFilePath.replace("\\", "/");
            if (isTextFile) {
                String plainText = Files.readString(localFilePath);
                encryptedText = CryptoManager.encryptText(plainText, ConfigManager.AES_ENCRYPTION_KEY);
                File tempEncryptedFile = File.createTempFile(localFile.getName() + "_encrypted_", ".txt");
                Files.writeString(tempEncryptedFile.toPath(), encryptedText);
                ftpManager.uploadFile(tempEncryptedFile, remoteFullPath + ".encrypted");
                tempEncryptedFile.delete();
                System.out.println("Text file encrypted and synchronized: " + localFilePath + " -> " + remoteFullPath + ".encrypted");
            } else {
                ftpManager.uploadFile(localFile, remoteFullPath);
                System.out.println("Binary file synchronized: " + localFilePath + " -> " + remoteFullPath);
            }
        } catch (Exception e) {
            System.err.println("Error synchronizing file " + localFilePath + " with FTP: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void deleteFileFromFTP(String remoteFilePath) {
        try {
            String remoteFullPath = remoteFilePath.replace("\\", "/");
            ftpManager.deleteFile(remoteFullPath);
            ftpManager.deleteFile(remoteFullPath + ".encrypted");
        } catch (Exception e) {
            System.err.println("Error deleting file " + remoteFilePath + " from FTP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isTextFile(Path filePath) {
        String filename = filePath.toString().toLowerCase();
        return filename.endsWith(".txt") || filename.endsWith(".json");
    }

}