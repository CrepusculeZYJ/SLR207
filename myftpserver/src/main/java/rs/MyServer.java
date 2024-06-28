package rs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.log4j.PropertyConfigurator;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class MyServer {

    private String[] server = {"tp-4b01-04", "tp-4b01-06", "tp-4b01-07"};

    private int serverID=0;
    private Map<String, Integer> wordTimes = new HashMap<>();
    private Map<String, Integer> hashedWordTimes = new HashMap<>();
    private Map<String, Integer> sortedWordTimes = new HashMap<>();
    List<Map.Entry<String, Integer>> sortedWordTimesList = null;

    private boolean DEBUG = false;

    private boolean complete1Flag = false;
    private boolean complete2Flag = false;

    class FTPServer {
        public void startServer ( ) {
            PropertyConfigurator.configure(MyServer.class.getResource("/log4J.properties"));
            FtpServerFactory serverFactory = new FtpServerFactory();
            int port = 7927; // Replace 3456 with the desired port number
    
            ListenerFactory listenerFactory = new ListenerFactory();
            listenerFactory.setPort(port);
    
            serverFactory.addListener("default", listenerFactory.createListener());
    
            // Create a UserManager instance
            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            File userFile = new File("users.properties");
            if (!userFile.exists()) {
                try {
                    if (userFile.createNewFile()) {
                        if (DEBUG) System.out.println("File created: " + userFile.getName());
                    } else {
                        if (DEBUG) System.out.println("File already exists.");
                    }
                } catch (IOException e) {
                    if (DEBUG) System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            }
            
            userManagerFactory.setFile(userFile); // Specify the file to store user details
            userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor()); // Store plain text passwords
            UserManager userManager = userManagerFactory.createUserManager();
            // Create a user
            BaseUser user = new BaseUser();
            user.setName("yuanjie"); // Replace "username" with the desired username
            user.setPassword("123456"); // Replace "password" with the desired password
            String username = user.getName();
            String homeDirectory = System.getProperty("java.io.tmpdir")  + "/" + username + "/files"; 
            File directory = new File(homeDirectory); // Convert the string to a File object
            if (!directory.exists()) { // Check if the directory exists
                if (directory.mkdirs()) {
                    if (DEBUG) System.out.println("Directory created: " + directory.getAbsolutePath());
                } else {
                    if (DEBUG) System.out.println("Failed to create directory.");
                }
            }
            user.setHomeDirectory(homeDirectory);
            // Set write permissions for the user
            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);
            user.setHomeDirectory(homeDirectory);
    
            // Add the user to the user manager
            try {
                userManager.save(user);
            } catch (FtpException e) {
                e.printStackTrace();
            }
            // Set the user manager on the server context
            serverFactory.setUserManager(userManager);
    
            FtpServer server = serverFactory.createServer();
    
            // start the server
            try {
                server.start();
                if (DEBUG) System.out.println("FTP Server started on port " + port);
                
            } catch (FtpException e) {
                e.printStackTrace();
            }
        }
    }
    
    class FTPServerThread extends Thread {
        public void run ( ) {
            FTPServer ftpServer = new FTPServer();
            ftpServer.startServer();
        }
    }

    class FTPServerConnection {

        String server;
        int port;
        String username;
        String password;
        
        public FTPServerConnection(String server, int port, String username, String password) {
            this.server = server;
            this.port = port;
            this.username = username;
            this.password = password;
        }
    
        public void sendFile (String filename, String content) {
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    
                // Code to display files
                FTPFile[] files = ftpClient.listFiles();
                boolean fileExists = false;
                for (FTPFile file : files) {
                    if (file.getName().equals(filename)) {
                        fileExists = true;
                        break;
                    }
                }
    
                if (!fileExists) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
                    ftpClient.storeFile(filename, inputStream);
                    int errorCode = ftpClient.getReplyCode();
                    if (errorCode != 226) {
                        System.out.println("File upload failed. FTP Error code: " + errorCode);
                    } else {
                        System.out.println("File uploaded successfully.");
                    }
                } else {
                    // Code to retrieve and display file content
                    InputStream inputStream = ftpClient.retrieveFileStream(filename);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    reader.close();
                    ftpClient.completePendingCommand();
                }
    
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        public void sendFileFromLocal(String localFilePath, String remoteFilename) {
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    
                try (InputStream inputStream = new FileInputStream(localFilePath)) {
                    System.out.println("Start uploading file: " + localFilePath);
                    boolean done = ftpClient.storeFile(remoteFilename, inputStream);
                    if (done) {
                        System.out.println("File uploaded successfully to " + remoteFilename);
                    } else {
                        System.out.println("Could not upload the file.");
                    }
                }
    
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    
        public void sendAllFilesInDirectory(String localDirectoryPath) {
            File directory = new File(localDirectoryPath);
    
            if (!directory.isDirectory()) {
                System.out.println(localDirectoryPath + " is not a directory.");
                return;
            }
    
            File[] files = directory.listFiles();
            if (files == null) {
                System.out.println("No files found in directory: " + localDirectoryPath);
                return;
            }
    
            for (File file : files) {
                if (file.isFile()) {
                    sendFileFromLocal(file.getAbsolutePath(), file.getName());
                }
            }
        }
    }
    
    class SocketServer {
    
        private int port;
    
        public SocketServer(int port) {
            this.port = port;
        }
    
        public void startServer ( ) {
     
            ServerSocket listener = null;
            int numOfClients = 0;
    
            try {
                listener = new ServerSocket(port);
                if (DEBUG) System.out.println("Server is waiting to accept user...");
    
                while (true) {
                    try {
                        Socket socketOfServer = listener.accept();
                        if (DEBUG) System.out.println("Accept a client!");
                        numOfClients++;
                        RequestProcessor rp = new RequestProcessor(socketOfServer, numOfClients);
                        rp.start();
                    } catch (IOException e) {
                        if (DEBUG) System.out.println("Error accepting client connection: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                if (DEBUG) System.out.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (listener != null && !listener.isClosed()) {
                    try {
                        listener.close();
                    } catch (IOException e) {
                        if (DEBUG) System.out.println("Error closing server socket: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    class RequestProcessor extends Thread {
        Socket socketOfServer;
        int id;
    
        public RequestProcessor (Socket socketOfServer, int id) {
            this.socketOfServer=socketOfServer;
            this.id = id;
        }

        public void readFile(String path) throws IOException {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                if (DEBUG) System.out.println("Directory does not exist: " + path);
                return;
            }
    
            Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (DEBUG) System.out.println("File: " + file);
                    String filename = file.getFileName().toString();
                    if (filename.equals("test.txt")) splitWords(file);
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("Failed to access file: " + file.toString() + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        System.err.println("Failed to access directory: " + dir.toString() + " (" + exc.getMessage() + ")");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        public void splitWords(Path file) {
            try {
                BufferedReader reader = Files.newBufferedReader(file);
                String line;
                MultiLanguageWordSegmenter segmenter = new MultiLanguageWordSegmenter();
                while ((line = reader.readLine()) != null) {
                    line = line.toLowerCase();
                    List<String> words = segmenter.segmentWords(line);
                    for (String word : words) {
                        if (wordTimes.containsKey(word)) {
                            wordTimes.put(word, wordTimes.get(word) + 1);
                        } else {
                            wordTimes.put(word, 1);
                        }
                    }
                }
                reader.close();
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        }
    
        public void run ( ) {
            int socketPort = 9681;
            try {
                String line;
                BufferedReader is;
                BufferedWriter os;
    
                is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
    
                SocketClient socketClient1=null, socketClient2=null;
     
                while (true) {
                    line = is.readLine();
                    
                    if (DEBUG) System.out.println("Received message from client "+id+": " + line);
                    
                    if (line.contains("[N0][NUM]")) {
                        // receive id from client
                        if (line.charAt(9)=='1') {
                            serverID=1;
                            socketClient1 = new SocketClient("tp-4b01-06", socketPort, "N1");
                            socketClient2 = new SocketClient("tp-4b01-07", socketPort, "N1");
                        }
                        else if (line.charAt(9)=='2') {
                            serverID=2;
                            socketClient1 = new SocketClient("tp-4b01-04", socketPort, "N2");
                            socketClient2 = new SocketClient("tp-4b01-07", socketPort, "N2");
                        }
                        else if (line.charAt(9)=='3') {
                            serverID=3;
                            socketClient1 = new SocketClient("tp-4b01-04", socketPort, "N3");
                            socketClient2 = new SocketClient("tp-4b01-06", socketPort, "N3");
                        }
                        os.write("Connection OK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.equals("[N0][READ]")) {
                        readFile("files");
                        os.write("Read OK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.equals("[N0][SHUFFLE1]")) {
                        try(BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("shuffle1_1.txt"), "UTF-8"));
                        BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("shuffle1_2.txt"), "UTF-8"))) {
                            for (Map.Entry<String, Integer> entry : wordTimes.entrySet()) {
                                String word = entry.getKey();
                                int occTimes = entry.getValue(), wordHash = word.hashCode();
                                if (serverID==1) {
                                    if (wordHash%3==0) {
                                        if (hashedWordTimes.containsKey(word)) {
                                            hashedWordTimes.put(word, hashedWordTimes.get(word) + occTimes);
                                        } else {
                                            hashedWordTimes.put(word, occTimes);
                                        }
                                    }
                                    else if (wordHash%3==1) {
                                        writer1.write(occTimes+";"+word);
                                        writer1.newLine();
                                    }
                                    else if (wordHash%3==2) {
                                        writer2.write(occTimes+";"+word);
                                        writer2.newLine();
                                    }
                                }
                                else if (serverID==2) {
                                    if (wordHash%3==0) {
                                        writer1.write(occTimes+";"+word);
                                        writer1.newLine();
                                    }
                                    else if (wordHash%3==1) {
                                        if (hashedWordTimes.containsKey(word)) {
                                            hashedWordTimes.put(word, hashedWordTimes.get(word) + occTimes);
                                        } else {
                                            hashedWordTimes.put(word, occTimes);
                                        }
                                    }
                                    else if (wordHash%3==2) {
                                        writer2.write(occTimes+";"+word);
                                        writer2.newLine();
                                    }
                                }
                                else if (serverID==3) {
                                    if (wordHash%3==0) {
                                        writer1.write(occTimes+";"+word);
                                        writer1.newLine();
                                    }
                                    else if (wordHash%3==1) {
                                        writer2.write(occTimes+";"+word);
                                        writer2.newLine();
                                    }
                                    else if (wordHash%3==2) {
                                        if (hashedWordTimes.containsKey(word)) {
                                            hashedWordTimes.put(word, hashedWordTimes.get(word) + occTimes);
                                        } else {
                                            hashedWordTimes.put(word, occTimes);
                                        }
                                    }
                                }
                            }
                        }
                        String username = "yuanjie";
                        String password = "123456";
                        int FTPPort = 7927;
                        FTPServerConnection myFTPServer1 = null;
                        FTPServerConnection myFTPServer2 = null;
                        if (serverID==1) {
                            myFTPServer1 = new FTPServerConnection(server[1], FTPPort, username, password);
                            myFTPServer2 = new FTPServerConnection(server[2], FTPPort, username, password);
                            myFTPServer1.sendFileFromLocal("shuffle1_1.txt", "shuffle1_result1.txt");
                            myFTPServer2.sendFileFromLocal("shuffle1_2.txt", "shuffle1_result1.txt");
                        }
                        else if (serverID==2) {
                            myFTPServer1 = new FTPServerConnection(server[0], FTPPort, username, password);
                            myFTPServer2 = new FTPServerConnection(server[2], FTPPort, username, password);
                            myFTPServer1.sendFileFromLocal("shuffle1_1.txt", "shuffle1_result1.txt");
                            myFTPServer2.sendFileFromLocal("shuffle1_2.txt", "shuffle1_result2.txt");
                        }
                        else if (serverID==3) {
                            myFTPServer1 = new FTPServerConnection(server[0], FTPPort, username, password);
                            myFTPServer2 = new FTPServerConnection(server[1], FTPPort, username, password);
                            myFTPServer1.sendFileFromLocal("shuffle1_1.txt", "shuffle1_result2.txt");
                            myFTPServer2.sendFileFromLocal("shuffle1_2.txt", "shuffle1_result2.txt");
                        }
                        socketClient1.sendMessage("[COMPLETE1]");
                        socketClient2.sendMessage("[COMPLETE1]");
                        System.out.println("Shuffle1 OK");
                        os.write("Shuffle1 OK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.contains("[COUNT]")) {
                        int fmin=-1, fmax=-1;
                        for (Map.Entry<String, Integer> entry : hashedWordTimes.entrySet()) {
                            int occTimes = entry.getValue();
                            if (fmin==-1 || occTimes<fmin) fmin=occTimes;
                            if (fmax==-1 || occTimes>fmax) fmax=occTimes;
                        }
                        os.write(fmin+";"+fmax);
                        os.newLine();
                        os.flush();
                    }
                    else if (line.contains("[SHUFFLE2]")) {
                        int threshold1 = 0, threshold2 = 0;
                        String thresholds = line.substring(14);
                        for (int i=0;i<thresholds.length();i++) {
                            if (thresholds.charAt(i)==';') {
                                threshold1 = Integer.parseInt(thresholds.substring(0, i));
                                threshold2 = Integer.parseInt(thresholds.substring(i+1));
                                break;
                            }
                        }
                        try(BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("shuffle2_1.txt"), "UTF-8"));
                        BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("shuffle2_2.txt"), "UTF-8"))) {
                            for (Map.Entry<String, Integer> entry : hashedWordTimes.entrySet()) {
                                String word = entry.getKey();
                                int occTimes = entry.getValue();
                                if (occTimes<=threshold1) {
                                    if (serverID==1) {
                                        if (sortedWordTimes.containsKey(word)) {
                                            sortedWordTimes.put(word, sortedWordTimes.get(word) + occTimes);
                                        } else {
                                            sortedWordTimes.put(word, occTimes);
                                        }
                                    }
                                    else {
                                        writer1.write(occTimes+";"+word);
                                        writer1.newLine();
                                    }
                                }
                                else if (occTimes>threshold1 && occTimes<=threshold2) {
                                    if (serverID==2) {
                                        if (sortedWordTimes.containsKey(word)) {
                                            sortedWordTimes.put(word, sortedWordTimes.get(word) + occTimes);
                                        } else {
                                            sortedWordTimes.put(word, occTimes);
                                        }
                                    }
                                    else if (serverID==1) {
                                        writer1.write(occTimes+";"+word);
                                        writer1.newLine();
                                    }
                                    else {
                                        writer2.write(occTimes+";"+word);
                                        writer2.newLine();
                                    }
                                }
                                else {
                                    if (serverID==3) {
                                        if (sortedWordTimes.containsKey(word)) {
                                            sortedWordTimes.put(word, sortedWordTimes.get(word) + occTimes);
                                        } else {
                                            sortedWordTimes.put(word, occTimes);
                                        }
                                    }
                                    else {
                                        writer2.write(occTimes+";"+word);
                                        writer2.newLine();
                                    }
                                }
                            }
                        }
                        String username = "yuanjie";
                        String password = "123456";
                        int FTPPort = 7927;
                        FTPServerConnection myFTPServer1 = null;
                        FTPServerConnection myFTPServer2 = null;
                        if (serverID==1) {
                            myFTPServer1 = new FTPServerConnection(server[1], FTPPort, username, password);
                            myFTPServer2 = new FTPServerConnection(server[2], FTPPort, username, password);
                            myFTPServer1.sendFileFromLocal("shuffle2_1.txt", "shuffle2_result1.txt");
                            myFTPServer2.sendFileFromLocal("shuffle2_2.txt", "shuffle2_result1.txt");
                        }
                        else if (serverID==2) {
                            myFTPServer1 = new FTPServerConnection(server[0], FTPPort, username, password);
                            myFTPServer2 = new FTPServerConnection(server[2], FTPPort, username, password);
                            myFTPServer1.sendFileFromLocal("shuffle2_1.txt", "shuffle2_result1.txt");
                            myFTPServer2.sendFileFromLocal("shuffle2_2.txt", "shuffle2_result2.txt");
                        }
                        else if (serverID==3) {
                            myFTPServer1 = new FTPServerConnection(server[0], FTPPort, username, password);
                            myFTPServer2 = new FTPServerConnection(server[1], FTPPort, username, password);
                            myFTPServer1.sendFileFromLocal("shuffle2_1.txt", "shuffle2_result2.txt");
                            myFTPServer2.sendFileFromLocal("shuffle2_2.txt", "shuffle2_result2.txt");
                        }
                        socketClient1.sendMessage("[COMPLETE2]");
                        socketClient2.sendMessage("[COMPLETE2]");
                        os.write("Shuffle2 OK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.contains("[COMPLETE1]")) {
                        if (!complete1Flag) {
                            complete1Flag =true;
                        }
                        else {
                            try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(new FileInputStream("files/shuffle1_result1.txt"), "UTF-8"));
                            BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream("files/shuffle1_result2.txt"), "UTF-8"))
                            ){
                                String line1, line2;
                                while ((line1 = reader1.readLine()) != null) {
                                    for (int i=0;i<line1.length();i++) {
                                        if (line1.charAt(i)==';') {
                                            String word = line1.substring(i+1);
                                            int occTimes = Integer.parseInt(line1.substring(0, i));
                                            if (hashedWordTimes.containsKey(word)) {
                                                hashedWordTimes.put(word, hashedWordTimes.get(word) + occTimes);
                                            } else {
                                                hashedWordTimes.put(word, occTimes);
                                            }
                                            break;
                                        }
                                    }
                                }
                                while ((line2 = reader2.readLine()) != null) {
                                    for (int i=0;i<line2.length();i++) {
                                        if (line2.charAt(i)==';') {
                                            String word = line2.substring(i+1);
                                            int occTimes = Integer.parseInt(line2.substring(0, i));
                                            if (hashedWordTimes.containsKey(word)) {
                                                hashedWordTimes.put(word, hashedWordTimes.get(word) + occTimes);
                                            } else {
                                                hashedWordTimes.put(word, occTimes);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        os.write("ACK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.contains("[COMPLETE2]")) {
                        if (!complete2Flag) {
                            complete2Flag =true;
                        }
                        else {
                            try (BufferedReader reader1 = new BufferedReader(new InputStreamReader(new FileInputStream("files/shuffle2_result1.txt"), "UTF-8"));
                            BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream("files/shuffle2_result2.txt"), "UTF-8"))
                            ){
                                String line1, line2;
                                while ((line1 = reader1.readLine()) != null) {
                                    for (int i=0;i<line1.length();i++) {
                                        if (line1.charAt(i)==';') {
                                            String word = line1.substring(i+1);
                                            int occTimes = Integer.parseInt(line1.substring(0, i));
                                            if (sortedWordTimes.containsKey(word)) {
                                                sortedWordTimes.put(word, sortedWordTimes.get(word) + occTimes);
                                            } else {
                                                sortedWordTimes.put(word, occTimes);
                                            }
                                            break;
                                        }
                                    }
                                }
                                while ((line2 = reader2.readLine()) != null) {
                                    for (int i=0;i<line2.length();i++) {
                                        if (line2.charAt(i)==';') {
                                            String word = line2.substring(i+1);
                                            int occTimes = Integer.parseInt(line2.substring(0, i));
                                            if (sortedWordTimes.containsKey(word)) {
                                                sortedWordTimes.put(word, sortedWordTimes.get(word) + occTimes);
                                            } else {
                                                sortedWordTimes.put(word, occTimes);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        os.write("ACK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.contains("[REQUEST]")) {
                        String fileName = "./Result/WordCount.txt";
                        Path path = Paths.get(fileName);
                        try {
                            if (Files.notExists(path.getParent())) {
                                Files.createDirectories(path.getParent());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sortedWordTimesList = new ArrayList<>(sortedWordTimes.entrySet());
                        sortedWordTimesList.sort(Map.Entry.comparingByValue(Collections.reverseOrder()));
                        
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                            for (Map.Entry<String, Integer> entry : sortedWordTimesList) {
                                String word = entry.getKey();
                                int occTimes = entry.getValue();
                                writer.write(word + ": " + occTimes);
                                writer.newLine();
                            }
                        } catch (IOException e) {
                            System.err.println("Error writing to file: " + e.getMessage());
                        }
                        
                        os.write("Request OK");
                        os.newLine();
                        os.flush();
                    }
                    else if (line.equals("[N0][QUIT]")) {
                        // quit socket connection
                        os.write("OK");
                        os.newLine();
                        os.flush();
                        break;
                    }
                    else {
                        os.write("Received: " + line);
                        os.newLine();
                        os.flush();
                    }
                }
            }
            catch (Exception e) {}
        }
    }
    
    class SocketServerThread extends Thread {
        private int port = 9681;
    
        public SocketServerThread ( ) {
        }
        
        public SocketServerThread (int port) {
            this.port = port;
        }
        
        public void run ( ) {
            SocketServer socketServer = new SocketServer(port);
            socketServer.startServer();
        }
    }
    
    class SocketClient {
        // Server Host
        public String serverHost = "localhost";
        public int port = 9681;
        public String name = "";
        private Socket socketOfClient;
        private BufferedWriter os;
        private BufferedReader is;
    
        public SocketClient (String serverHost, int port, String name) {
            this.serverHost = serverHost;
            this.port = 9681;
            
            name="["+name+"]";
            this.name = name;
            
            try {
                socketOfClient = new Socket(serverHost, port);
            }
            catch (UnknownHostException e) {
                System.err.println("Don't know about host " + serverHost);
                return;
            }
            catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + serverHost);
                return;
            }
    
            try {
                os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
                is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
            }
            catch (Exception e) {}
        }
    
        public String sendMessage (String message) {
            try {
                os.write(name+message);
                os.newLine();
                os.flush();
            }
            catch (Exception e) {
                System.err.println("Error sending message");
                return null;
            }
    
            try {
                String response = is.readLine();
                return response;
            }
            catch (Exception e) {
                System.err.println("Error receiving message");
                return null;
            }
        }

        public String sendMessage (String message, boolean shouldFlush) {
            try {
                os.write(name+message);
                os.newLine();
                if (shouldFlush) os.flush();
            }
            catch (Exception e) {
                System.err.println("Error sending message");
                return null;
            }
            
            if (shouldFlush) {
                try {
                    String response = is.readLine();
                    return response;
                }
                catch (Exception e) {
                    System.err.println("Error receiving message");
                    return null;
                }
            }
            else return null;
        }

        public void flushBuffer ( ) {
            try {
                os.flush();
            }
            catch (Exception e) {
                System.err.println("Error flushing buffer");
            }
        }
    }
    
    class MultiLanguageWordSegmenter {
    
        public List<String> segmentWords(String text) {
            List<String> words = new ArrayList<>();
            StringBuilder word = new StringBuilder();
    
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
    
                if (Character.isWhitespace(ch)) {
                    if (word.length() > 0) {
                        words.add(word.toString());
                        word.setLength(0);
                    }
                } else if (isCJKCharacter(ch)) {
                    if (word.length() > 0) {
                        words.add(word.toString());
                        word.setLength(0);
                    }
                    words.add(String.valueOf(ch));
                } else if (isWordCharacter(ch)) {
                    word.append(ch);
                } else {
                    if (word.length() > 0) {
                        words.add(word.toString());
                        word.setLength(0);
                    }
                    words.add(String.valueOf(ch));
                }
            }
    
            if (word.length() > 0) {
                words.add(word.toString());
            }
    
            return words;
        }
    
        // check if words
        public boolean isWordCharacter(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '\'';
        }
    
        // check if CJK character
        public boolean isCJKCharacter(char ch) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                   block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                   block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                   block == Character.UnicodeBlock.HIRAGANA ||
                   block == Character.UnicodeBlock.KATAKANA ||
                   block == Character.UnicodeBlock.HANGUL_SYLLABLES;
        }
    }

    public static void main(String[] args) {
        MyServer myServer = new MyServer();
        FTPServerThread ftpServerThread = myServer.new FTPServerThread();
        ftpServerThread.start();
        SocketServerThread socketServerThread = myServer.new SocketServerThread();
        socketServerThread.start();
    }
}