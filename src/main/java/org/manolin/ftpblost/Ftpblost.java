package org.manolin.ftpblost;

import org.manolin.ftpblost.controller.BackupController;
import org.manolin.ftpblost.logs.LogsManager;

/**
 *
 * @author manu
 */
public class Ftpblost {

    public static void main(String[] args) {
        LogsManager.logInfo("Starting FTPBlost application...");
        try {
            BackupController backupController = new BackupController();
            backupController.showMenu();
        } catch (Exception e) {
            LogsManager.logError("Fatal error in application", e);
            System.exit(1);
        }
    }
}
