package org.example;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.*;
import org.example.ui.LoginFrame;

import javax.net.ssl.SSLSocket;

public class App {

    public static Client client;
    public static JFrame frame;

    public App() {
        try {
            SwingUtilities.invokeLater(LoginFrame::new);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);
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
        // Création de la fenêtre
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(App.frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setPreferredSize(new Dimension(400, 300));
        frame.pack();
        frame.setVisible(true);
        client = new Client();
        SwingUtilities.invokeLater(App::new);

    }
}
