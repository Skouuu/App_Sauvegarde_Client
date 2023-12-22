package org.example.ui;

import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


import org.example.App;

public class LoginFrame {

    public LoginFrame() {
        createWindows();
    }

    public void createWindows() {
        // Réglages finaux de la fenêtre
        App.frame.pack();
        App.frame.setVisible(true);
        // Clear previous content
        App.frame.getContentPane().removeAll();
        App.frame.repaint();

        // Panel pour le champ de nom
        JPanel usernamePanel = new JPanel();
        JTextField usernameField = new JTextField(20);
        JLabel usernameLabel = new JLabel("Entrez votre username : ");
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);

        // Panel pour le champ mot de passe
        JPanel passwordPanel = new JPanel();
        JPasswordField passwordField = new JPasswordField(20);
        JLabel passwordLabel = new JLabel("Entrez votre mot de passe : ");
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);

        // Panel pour le bouton Submit
        JPanel submitPanel = new JPanel();
        JButton submitButton = new JButton("Submit");
        submitPanel.add(submitButton);

        // Ajout des panels à la fenêtre
        App.frame.add(usernamePanel);
        App.frame.add(passwordPanel);
        App.frame.add(submitPanel);

        submitButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            App.client.setUsername(username);
            try {
                App.client.getDataOutputStream().writeUTF(username);
                App.client.getDataOutputStream().writeUTF(password);
                String validationConnexion;
                validationConnexion = App.client.getDataInputStream().readUTF();
                boolean isAuthenticated;
                System.out.println(validationConnexion);
                if (validationConnexion.equals("Connecté")){
                    isAuthenticated = true;
                }
                else {
                    isAuthenticated = false;
                }
                if (isAuthenticated) {
                    File paramFile = new File("parametres.txt");
                    if (!paramFile.exists()) {
                        SwingUtilities.invokeLater(ConfigFrame::new); // Ouvrir la fenêtre principale
                    } else {
                        SwingUtilities.invokeLater(BackupFrame::new);

                    }
                } else {
                    JOptionPane.showMessageDialog(App.frame, "Vous n'avez pas entrée des identifiants valide", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        App.frame.revalidate();
        App.frame.repaint();
    }

}
