package org.example;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

import org.example.ui.BackupFrame;
import org.example.ui.ConfigFrame;
import org.example.ui.LoginFrame;

import javax.net.ssl.SSLSocket;

public class App {

    public static Client client;
    public static SSLSocket socket;
    public static JFrame frame;

    public App() {
        // Création de la fenêtre
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(App.frame.getContentPane(), BoxLayout.Y_AXIS));
        try {
            SwingUtilities.invokeLater(LoginFrame::new);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);
                    // Fermez la socket ici
                    try {
                        if (client.getSocket() != null && !client.getSocket().isClosed()) {
                            client.getSocket().close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }


    public static void main(String[] args) {
        frame = new JFrame("Backup Application");
        client = new Client();
        try {
            client.loadSecretKeyIfExists("AES", "secret_key.txt");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SwingUtilities.invokeLater(App::new);

    }
}
