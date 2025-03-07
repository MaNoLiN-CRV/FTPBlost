package org.manolin.ftpblost.managers;



public class ConfigManager {

    public static final String LOCAL_DIRECTORY_TO_WATCH = "/home/manel/ftprueba";
    public static final String FTP_SERVER = "localhost";
    public static final int FTP_PORT = 21; // FTP DEFAULT PORT
    public static final String FTP_USER = "manel";
    public static final String FTP_PASSWORD = "elvergeles";
    public static final String FTP_REMOTE_BASE_DIRECTORY = "/";
    public static final String AES_ENCRYPTION_KEY = CryptoManager.generateAES(256); // Military-grade AES key
    public static final int THREAD_POOL_SIZE = 2;// <-- Number of threads for file monitoring event processing
    }