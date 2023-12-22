package org.example.ui;

import org.example.App;
import org.example.Client;

import javax.swing.*;
import java.io.File;


public class BackupFrame extends JFrame {

    public BackupFrame() {
        createWindows();
    }


    public void createWindows() {

        // Clear previous content
        App.frame.getContentPane().removeAll();
        App.frame.repaint();
        // Panel pour le champ de nom
        JLabel questionLabel = new JLabel("Que voulez-vous faire ?");

        // Panel pour les boutons radio
        JPanel radioPanel = new JPanel();
        JRadioButton backupButton = new JRadioButton("Sauvegarder", true);
        JRadioButton restoreButton = new JRadioButton("Restaurer");
        ButtonGroup operationGroup = new ButtonGroup();
        operationGroup.add(backupButton);
        operationGroup.add(restoreButton);
        radioPanel.add(backupButton);
        radioPanel.add(restoreButton);


        /// Panel pour le bouton de sélection de dossier
        JPanel folderButtonPanel = new JPanel();
        JButton folderButton = new JButton("Choisissez un dossier");
        folderButtonPanel.add(folderButton);

        // Panel pour afficher le chemin du dossier sélectionné
        JPanel folderLabelPanel = new JPanel();
        JLabel folderLabel = new JLabel("Rien n'est choisi");
        folderLabelPanel.add(folderLabel);

        // Panel pour le bouton Submit
        JPanel submitPanel = new JPanel();
        JButton submitButton = new JButton("Continuer");
        submitPanel.add(submitButton);

        // Action du bouton Submit
        submitButton.addActionListener(e -> {
            String folderPath = folderLabel.getText();

            // Vérifiez si le nom est rempli et qu'un dossier a été sélectionné
            if (!folderPath.equals("Rien n'est choisi")) {
                // Traitement des données
                boolean isBackup = backupButton.isSelected();

                if (isBackup) {
                    App.client.doBackup(folderPath);
                } else {
                    App.client.doRestore();
                }
                // Remplacez Socket par SSLSocket et SocketFactory par SSLSocketFactory

                // Fermeture de la fenêtre
                //frame.dispose();
                // Quand la fenêtre se ferme, on supprime tout dans le fichier avec des suffixes
            } else {
                // Affichage d'un message d'erreur
                JOptionPane.showMessageDialog(App.frame, "Veuillez remplir tous les champs.", "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Sélecteur de dossier
        folderButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showOpenDialog(App.frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = fileChooser.getSelectedFile();
                folderLabel.setText(selectedFolder.getAbsolutePath());
            }
        });

        // Ajout des panels à la fenêtre
        App.frame.add(questionLabel);
        App.frame.add(radioPanel);

        App.frame.add(folderButtonPanel);
        App.frame.add(folderLabelPanel);
        App.frame.add(submitPanel);

        App.frame.revalidate();
        App.frame.repaint();
    }
}
