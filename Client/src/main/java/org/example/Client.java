package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocket;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;

public class Client {
    private static DataOutputStream dataOutputStream;
    private static DataInputStream dataInputStream;

    private static SSLSocket socket;
    private static SecretKey secretKey;

    private String username;
    private String path;

    public Client() {
        dataOutputStream = null;
        dataInputStream = null;
        this.username = "";
        this.path = "";
        socket = initSSLSocket();
    }

    public SSLSocket getSocket() {
        return socket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDataOutputStream(DataOutputStream d) {
        dataOutputStream = d;
    }

    public void setDataInputStream(DataInputStream d) {
        dataInputStream = d;
    }

    public DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

//    public static void main(String[] args)
//            throws NoSuchAlgorithmException, InvalidKeySpecException {
//        System.setProperty("javax.net.ssl.trustStore", "clienttruststore.jks");
//        System.setProperty("javax.net.ssl.trustStorePassword", "app_sauvegarde");
//        // Création de la fenêtre
//        JFrame frame = new JFrame("Backup Application");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
//        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//
//        try {
//            secretKey = loadSecretKeyIfExists("AES", "secret_key.txt");
//            SSLSocket SSLsocket = (SSLSocket) factory.createSocket("127.0.0.1", 6666);
//            socket = SSLsocket;
//            dataOutputStream = new DataOutputStream(socket.getOutputStream());
//            dataInputStream = new DataInputStream(socket.getInputStream());
//            showLoginInterface(frame);
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
//
//        frame.addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                super.windowClosing(e);
//                // Fermez la socket ici
//                try {
//                    if (socket != null && !socket.isClosed()) {
//                        socket.close();
//                    }
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
//
//        // Réglages finaux de la fenêtre
//        frame.pack();
//        frame.setVisible(true);
//    }

    public SSLSocket initSSLSocket() {
        System.setProperty("javax.net.ssl.trustStore", "clienttruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "app_sauvegarde");
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            socket = (SSLSocket) factory.createSocket("127.0.0.1", 6666);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            return socket;
            // Reste du code...
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }


    public boolean isAuthenticated(String $password) {
        try {
            dataOutputStream.writeUTF(username);// Envoi du nom d'utilisateur
            String storedPasswordHash = dataInputStream.readUTF();
            boolean isAuthenticated = HashingPassword.validatePassword($password, storedPasswordHash);
            return isAuthenticated;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void doBackup(String path) {
        // Envoi du nom du client et du chemin du dossier de sauvegarde
        try {
            dataOutputStream.writeUTF("SAVE");
            dataOutputStream.writeUTF(path);
            System.out.println("Sending files to the Server");

            Instant lastBackupTime = readLastBackupTime();
            savePaths(Paths.get(path), lastBackupTime); // Modifier les chemins sauvegardés
            sendFilesListedInFile("paths.txt", path); // chemin vers votre fichier .txt
            deleteFile("paths.txt"); // Supprimer le fichier après la sauvegarde
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doRestore() {
        // Envoi du nom du client et de la demande de restauration
        try {
            dataOutputStream.writeUTF("RESTORE");
            dataOutputStream.writeUTF(this.path);
            receiveFiles(this.path, secretKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void sendFilesListedInFile(String listFilePath, String directoryPath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(listFilePath));
        String filePath;

        while ((filePath = br.readLine()) != null) {
            System.out.println(filePath);
            sendFile(filePath, directoryPath, secretKey);
        }

        dataOutputStream.writeUTF("END");

        br.close();
    }

    public static void sendFile(String filePath, String directoryPath, SecretKey key) throws Exception {
        File file = new File(filePath);
        Path basePath = Paths.get(directoryPath);
        String relativePath = basePath.relativize(file.toPath()).toString(); // Chemin relatif

        dataOutputStream.writeUTF(relativePath);
        dataOutputStream.writeLong(file.length());

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4 * 1024];
        int bytes;
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            byte[] encryptedData = EncryptionSystem.encryptData(Arrays.copyOf(buffer, bytes),
                    loadSecretKey("AES", "secret_key.txt"));
            dataOutputStream.writeInt(encryptedData.length);
            dataOutputStream.write(encryptedData);
            dataOutputStream.flush();
        }
        fileInputStream.close();
    }

    public static void savePaths(Path repertoire, Instant lastBackupTime) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("paths.txt"))) {
            Files.walkFileTree(repertoire, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Instant fileModifiedTime = attrs.lastModifiedTime().toInstant();
                    if (fileModifiedTime.isAfter(lastBackupTime) && filterFileBySuffix(file)) {
                        writer.write(file.toRealPath().toString());
                        writer.newLine();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void receiveFiles(String restorePath, SecretKey key) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        String fileName;
        while (!(fileName = dataInputStream.readUTF()).equals("END")) {
            int expectedDataLength = dataInputStream.readInt(); // Longueur attendue des données chiffrées
            byte[] encryptedData = new byte[expectedDataLength]; // Créez un tableau de bytes de la taille attendue

            int actualDataLength = dataInputStream.read(encryptedData); // Lire les données chiffrées

            // Vérifiez si la longueur des données lues correspond à la longueur attendue
            if (actualDataLength != expectedDataLength) {
                throw new IOException("Incomplete data read. Expected " + expectedDataLength
                        + " bytes, but got " + actualDataLength + " bytes.");
            }
            byte[] decryptedData;
            try {
                decryptedData = EncryptionSystem.decryptData(encryptedData, loadSecretKey("AES", "secret_key.txt"));
                Path filePath = Paths.get(restorePath, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, decryptedData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void storeSecretKeyIfNotExists(SecretKey secretKey, String filePath) throws Exception {
        Path path = Paths.get(filePath);

        // Vérifier si le fichier existe déjà
        if (!Files.exists(path)) {
            // Convertir la clé secrète en chaîne de caractères en utilisant Base64
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            // Stocker la clé encodée dans un fichier
            Files.write(path, encodedKey.getBytes());
            System.out.println("Key stored successfully.");
        } else {
            System.out.println("Key file already exists.");
        }
    }

    public static SecretKey loadSecretKey(String algorithm, String filePath) throws Exception {
        // Lire la chaîne encodée depuis le fichier
        byte[] encodedKey = Files.readAllBytes(Paths.get(filePath));

        // Convertir la chaîne encodée en clé secrète
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, algorithm);
    }

    public SecretKey loadSecretKeyIfExists(String algorithm, String filePath) throws Exception {
        Path path = Paths.get(filePath);

        // Vérifier si le fichier de la clé existe
        if (Files.exists(path)) {
            // Charger la clé existante
            return loadSecretKey(algorithm, filePath);
        } else {
            // Générer une nouvelle clé et la stocker
            SecretKey newKey = EncryptionSystem.generateSecretKey();
            storeSecretKeyIfNotExists(newKey, filePath);
            return newKey;
        }
    }

    // Fonction pour filtrer les fichiers en fonction des suffixes définis dans le
    // fichier de paramètres
    public static boolean filterFileBySuffix(Path file) {
        try (BufferedReader reader = new BufferedReader(new FileReader("parametres.txt"))) {
            // Lire les suffixes à partir du fichier de paramètres
            Set<String> suffixes = new HashSet<>();
            String suffix;
            while ((suffix = reader.readLine()) != null) {
                suffixes.add(suffix.trim().toLowerCase()); // Stocker les suffixes en minuscules pour la correspondance
                // insensible à la casse
            }

            // Vérifier si le fichier a l'un des suffixes spécifiés
            String fileName = file.getFileName().toString();
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex != -1) {
                String fileSuffix = fileName.substring(lastDotIndex + 1).toLowerCase();
                return suffixes.contains(fileSuffix);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addSuffixesToFile(Set<String> suffixes) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("parametres.txt", true))) {
            for (String suffix : suffixes) {
                writer.write(suffix.trim().toLowerCase()); // Stocker les suffixes en minuscules pour la correspondance
                // insensible à la casse
                writer.newLine(); // Nouvelle ligne pour chaque suffixe
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeSuffixesFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("parametres.txt", false))) {
            // Effacez le contenu du fichier en écrivant une chaîne vide
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Instant readLastBackupTime() {
        try {
            String lastBackupString = dataInputStream.readUTF();
            return Instant.parse(lastBackupString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Instant.MIN; // Retourner une date antérieure en cas d'erreur
    }

    public static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            System.out.println("File " + filePath + " deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to delete " + filePath);
        }
    }

//    private static void showLoginInterface(JFrame frame) {
//        // Clear previous content
//        frame.getContentPane().removeAll();
//        frame.repaint();
//
//        // Panel pour le champ de nom
//        JPanel usernamePanel = new JPanel();
//        JTextField usernameField = new JTextField(20);
//        JLabel usernameLabel = new JLabel("Enter Your Username: ");
//        usernamePanel.add(usernameLabel);
//        usernamePanel.add(usernameField);
//
//        // Panel pour le champ mot de passe
//        JPanel passwordPanel = new JPanel();
//        JPasswordField passwordField = new JPasswordField(20);
//        JLabel passwordLabel = new JLabel("Enter Your Password: ");
//        passwordPanel.add(passwordLabel);
//        passwordPanel.add(passwordField);
//
//        // Panel pour le bouton Submit
//        JPanel submitPanel = new JPanel();
//        JButton submitButton = new JButton("Submit");
//        submitPanel.add(submitButton);
//
//        // Ajout des panels à la fenêtre
//        frame.add(usernamePanel);
//        frame.add(passwordPanel);
//        frame.add(submitPanel);
//
//        submitButton.addActionListener(e -> {
//            try {
//                String username = usernameField.getText().trim();
//                dataOutputStream.writeUTF(username); // Envoi du nom d'utilisateur
//                String storedPasswordHash = dataInputStream.readUTF();
//                boolean isAuthenticated = HashingPassword.validatePassword(
//                        new String(passwordField.getPassword()), storedPasswordHash);
//                if (isAuthenticated) {
//                    showBackupRestoreInterface(frame, username); // Afficher l'interface de sauvegarde/restauration
//                } else {
//                    JOptionPane.showMessageDialog(frame, "Vous n'avez pas entrée des identifiants valide", "Error",
//                            JOptionPane.ERROR_MESSAGE);
//                }
//            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
//                ex.printStackTrace();
//            }
//        });
//        frame.revalidate();
//        frame.repaint();
//    }
//
//    private static void showBackupRestoreInterface(JFrame frame, String username) {
//        // Clear previous content
//        frame.getContentPane().removeAll();
//        frame.repaint();
//
//        // Panel pour les boutons radio
//        JPanel radioPanel = new JPanel();
//        JRadioButton backupButton = new JRadioButton("Sauvegarde", true);
//        JRadioButton restoreButton = new JRadioButton("Restauration");
//        ButtonGroup operationGroup = new ButtonGroup();
//        operationGroup.add(backupButton);
//        operationGroup.add(restoreButton);
//        radioPanel.add(backupButton);
//        radioPanel.add(restoreButton);
//
//        // Panel pour les cases à cocher des suffixes
//        JPanel suffixPanel = new JPanel();
//        JLabel suffixLabel = new JLabel("Select File Suffixes to Save:");
//        suffixPanel.add(suffixLabel);
//
//        // Ajoutez autant de cases à cocher que nécessaire en fonction de vos besoins
//        JCheckBox txtCheckBox = new JCheckBox("txt");
//        JCheckBox jpgCheckBox = new JCheckBox("jpg");
//        JCheckBox pdfCheckBox = new JCheckBox("pdf");
//
//        // Ajoutez les cases à cocher au panneau
//        suffixPanel.add(txtCheckBox);
//        suffixPanel.add(jpgCheckBox);
//        suffixPanel.add(pdfCheckBox);
//
//        /// Panel pour le bouton de sélection de dossier
//        JPanel folderButtonPanel = new JPanel();
//        JButton folderButton = new JButton("Select Folder");
//        folderButtonPanel.add(folderButton);
//
//        // Panel pour afficher le chemin du dossier sélectionné
//        JPanel folderLabelPanel = new JPanel();
//        JLabel folderLabel = new JLabel("No folder selected");
//        folderLabelPanel.add(folderLabel);
//
//        // Panel pour le bouton Submit
//        JPanel submitPanel = new JPanel();
//        JButton submitButton = new JButton("Submit");
//        submitPanel.add(submitButton);
//
//        // Sélecteur de dossier
//        folderButton.addActionListener(e -> {
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//            int option = fileChooser.showOpenDialog(frame);
//            if (option == JFileChooser.APPROVE_OPTION) {
//                File selectedFolder = fileChooser.getSelectedFile();
//                folderLabel.setText(selectedFolder.getAbsolutePath());
//            }
//        });
//
//        // Ajouter les composants pour la sauvegarde et la restauration...
//        // radioPanel, suffixPanel, folderButtonPanel, etc.
//
//        // Action du bouton Submit pour la sauvegarde/restauration
//        submitButton.addActionListener(e -> {
//            try {
//                String folderPath = folderLabel.getText();
//
//                // Vérifiez si le nom est rempli et qu'un dossier a été sélectionné
//                if (!folderPath.equals("No folder selected")) {
//                    // Traitement des données
//                    boolean isBackup = backupButton.isSelected();
//
//                    // Récupérez les suffixes à partir des cases à cocher
//                    Set<String> suffixes = new HashSet<>();
//                    if (txtCheckBox.isSelected())
//                        suffixes.add("txt");
//                    if (jpgCheckBox.isSelected())
//                        suffixes.add("jpg");
//                    if (pdfCheckBox.isSelected())
//                        suffixes.add("pdf");
//                    addSuffixesToFile(suffixes);
//
//                    if (isBackup) {
//                        dataOutputStream.writeUTF("SAVE");
//                        dataOutputStream.writeUTF(folderPath);
//                        Instant lastBackupTime = readLastBackupTime();
//                        System.out.println("Sending files to the Server");
//
//                        savePaths(Paths.get(folderPath), lastBackupTime); // Modifier le chemins sauvegardés
//                        sendFilesListedInFile("paths.txt", folderPath); // chemin vers votre fichier .txt
//                        deleteFile("paths.txt"); // Supprimer le fichier après la sauvegarde
//                    } else {
//                        // Envoi du nom du client et de la demande de restauration
//                        dataOutputStream.writeUTF("RESTORE");
//                        dataOutputStream.writeUTF(folderPath);
//                        receiveFiles(folderPath, secretKey);
//                    }
//
//                    dataOutputStream.close();
//                    dataInputStream.close();
//                    // Fermeture de la fenêtre
//                    frame.dispose();
//                    // Quand la fenêtre se ferme, on supprime tout dans le fichier avec des suffixes
//                    removeSuffixesFile();
//                } else {
//                    // Affichage d'un message d'erreur
//                    JOptionPane.showMessageDialog(frame, "Veuillez remplir tous les champs.", "Erreur",
//                            JOptionPane.ERROR_MESSAGE);
//                }
//            } catch (IOException ex) {
//                ex.printStackTrace();
//                JOptionPane.showMessageDialog(frame, "Error in file transmission.", "Error", JOptionPane.ERROR_MESSAGE);
//            } catch (Exception e1) {
//                e1.printStackTrace();
//                JOptionPane.showMessageDialog(frame, "Error in file transmission.", "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        });
//
//        frame.add(radioPanel);
//        frame.add(suffixPanel);
//        frame.add(folderButtonPanel);
//        frame.add(folderLabelPanel);
//        frame.add(submitPanel);
//
//        frame.revalidate();
//        frame.repaint();
//    }
}