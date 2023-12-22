package org.example.ui;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.net.ssl.SSLSocket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


import org.example.App;
import org.example.Client;
import org.example.HashingPassword;

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
            try {
                String username = usernameField.getText().trim();
                App.client.getDataOutputStream().writeUTF(username); // Envoi du nom d'utilisateur
                String storedPasswordHash = App.client.getDataInputStream().readUTF();
                boolean isAuthenticated = HashingPassword.validatePassword(
                        new String(passwordField.getPassword()), storedPasswordHash);
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
            } catch (IOException | NoSuchAlgorithmException ex) {
                ex.printStackTrace();
            } catch (InvalidKeySpecException e1) {
                e1.printStackTrace();
            }
        });
        App.frame.revalidate();
        App.frame.repaint();
    }

}
