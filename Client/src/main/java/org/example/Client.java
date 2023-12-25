package org.example;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLSocketFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSocket;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    /**
     * Initie la SSL socket
     *
     * @return
     */
    public SSLSocket initSSLSocket() {
        System.setProperty("javax.net.ssl.trustStore", "clienttruststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "app_sauvegarde");
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            socket = (SSLSocket) factory.createSocket("127.0.0.1", 6666);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            return socket;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    /**
     * Effectue toute les actions nécessaires à la sauvegarde
     *
     * @param path
     */
    public void doBackup(String path) {
        try {
            dataOutputStream.writeUTF("SAVE");
            dataOutputStream.writeUTF(path);
            System.out.println("Sending files to the Server");

            Instant lastBackupTime = readLastBackupTime();
            savePaths(Paths.get(path), lastBackupTime);
            sendFilesListedInFile("paths.txt", path);
            deleteFile("paths.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Effectue toute les actions nécessaires à la restauration
     *
     * @param path
     */
    public void doRestore(String path) {
        try {
            dataOutputStream.writeUTF("RESTORE");
            dataOutputStream.writeUTF(path);
            receiveFiles(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Permet d'envoyer dont les chemins absolue sont écrit ligne par ligne dans un fichiers listFilePath
     *
     * @param listFilePath
     * @param directoryPath
     * @throws Exception
     */
    public static void sendFilesListedInFile(String listFilePath, String directoryPath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(listFilePath));
        String filePath;

        while ((filePath = br.readLine()) != null) {
            System.out.println(filePath);
            sendFile(filePath, directoryPath);
        }

        dataOutputStream.writeUTF("END");

        br.close();
    }

    /**
     * Envoie les bytes d'un fichier à travers des SSL socket
     *
     * @param filePath
     * @param directoryPath
     * @throws Exception
     */
    public static void sendFile(String filePath, String directoryPath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            System.err.println("Le fichier n'existe pas ou ne peut pas être lu: " + filePath);
            return;
        }

        Path basePath = Paths.get(directoryPath);
        String relativePath = basePath.relativize(file.toPath()).toString();
        System.out.println("Envoi du fichier: " + file.getAbsolutePath() + " avec chemin relatif: " + relativePath);


        dataOutputStream.writeUTF(relativePath); // Envoi du chemin relatif
        dataOutputStream.writeLong(file.length()); // Envoi de la taille du fichier

        // Envoi du contenu du fichier
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4 * 1024]; // Taille du tampon
        int bytes;
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close(); // Fermeture du flux d'entrée de fichier
    }

    /**
     * Sauvegarde les chemins des fichiers qui ont été modifé depuis le lastBackupTime
     *
     * @param repertoire
     * @param lastBackupTime
     */
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

    /**
     * Gère la réception est l'enregistrement des fichiers reçu lors d'un restauration
     */
    public static void receiveFiles(String restorePath) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        String fileName;
        while (!(fileName = dataInputStream.readUTF()).equals("END")) {
            long fileSize = dataInputStream.readLong();
            Path filePath = Paths.get(restorePath, fileName);
            filePath.getParent().toFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[4 * 1024];
                int bytes;
                while (fileSize > 0 && (bytes = dataInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytes);
                    fileSize -= bytes;
                }
            }
            System.out.println("Received file: " + fileName);
        }
    }

    /**
     * Filtre les fichiers en fonction des suffixes définis dans le fichier de paramètres
     *
     * @param file
     * @return boolean
     */
    public static boolean filterFileBySuffix(Path file) {
        try (BufferedReader reader = new BufferedReader(new FileReader("parametres.txt"))) {
            // Lire les suffixes à partir du fichier de paramètres
            Set<String> suffixes = new HashSet<>();
            String suffix;
            while ((suffix = reader.readLine()) != null) {
                suffixes.add(suffix.trim().toLowerCase()); // Stocker les suffixes en minuscules pour la correspondance
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

    /**
     * Ajoute les suffixes à sauvegarder dans un fichier
     *
     * @param suffixes
     */
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

    /**
     * Permet de gérer la récupération de la dernière date d'update envoyé par le serveur
     *
     * @return
     */
    public static Instant readLastBackupTime() {
        try {
            String lastBackupString = dataInputStream.readUTF();
            return Instant.parse(lastBackupString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Instant.MIN; // Retourner une date antérieure en cas d'erreur
    }

    /**
     * Permet de supprimer un fichier en fonction de son chemin absolue
     *
     * @param filePath
     */
    public static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            System.out.println("File " + filePath + " deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to delete " + filePath);
        }
    }
}