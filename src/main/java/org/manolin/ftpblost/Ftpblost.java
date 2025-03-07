package org.manolin.ftpblost;

import org.manolin.ftpblost.controller.BackupController;

/**
 *
 * @author manu
 */
public class Ftpblost {

    public static void main(String[] args) {
        BackupController backupController = new BackupController();
        backupController.showMenu();
    }
}
