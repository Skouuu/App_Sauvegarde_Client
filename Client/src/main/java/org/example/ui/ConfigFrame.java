package org.example.ui;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


import org.example.App;
import org.example.Client;

import java.util.HashSet;
import java.util.Set;

import static org.example.App.frame;

public class ConfigFrame extends JFrame {

    public ConfigFrame() {
        createWindows();
    }

    public void createWindows() {
        // Clear previous content
        frame.getContentPane().removeAll();
        frame.repaint();
        // Création de la fenêtre
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));


        // Panel pour les cases à cocher des suffixes
        JPanel chooseSuffixPanel = new JPanel();
        JLabel chooseSuffixLabel = new JLabel("Choisissez les extensions des fichiers à sauvegarder :");
        chooseSuffixPanel.add(chooseSuffixLabel);

        // Ajoutez autant de cases à cocher que nécessaire en fonction de vos besoins
        JCheckBox txtCheckBox = new JCheckBox("txt");
        JCheckBox jpgCheckBox = new JCheckBox("jpg");
        JCheckBox pdfCheckBox = new JCheckBox("pdf");
        JCheckBox zipCheckBox = new JCheckBox("zip");
        JCheckBox pngCheckBox = new JCheckBox("png");

        // Ajoutez les cases à cocher au panneau
        chooseSuffixPanel.add(txtCheckBox);
        chooseSuffixPanel.add(jpgCheckBox);
        chooseSuffixPanel.add(pdfCheckBox);
        chooseSuffixPanel.add(zipCheckBox);
        chooseSuffixPanel.add(pngCheckBox);

        JPanel addSuffixPanel = new JPanel();
        JLabel addSuffixLabel = new JLabel("Veillez écrire les extensions manquantes :");
        JTextArea addSuffixText = new JTextArea();
        //AJOUTER SES EXTENSIONS
        addSuffixPanel.add(addSuffixLabel);


        // Ajout d'un bouton "Continuer" qui ouvrira la fenêtre principale
        JButton continueButton = new JButton("Continuer");
        continueButton.addActionListener(e -> {
            // Récupérez les suffixes à partir des cases à cocher
            Set<String> suffixes = new HashSet<>();
            if (txtCheckBox.isSelected())
                suffixes.add("txt");
            if (jpgCheckBox.isSelected())
                suffixes.add("jpg");
            if (pdfCheckBox.isSelected())
                suffixes.add("pdf");
            App.client.addSuffixesToFile(suffixes);
            SwingUtilities.invokeLater(BackupFrame::new);
        });

        frame.add(chooseSuffixPanel);
        frame.add(addSuffixPanel);
        frame.add(addSuffixText);
        frame.add(continueButton);
        // Ajout du bouton à la fenêtre
        //getContentPane().add(continueButton, BorderLayout.CENTER);

        App.frame.revalidate();
        App.frame.repaint();
    }
}
