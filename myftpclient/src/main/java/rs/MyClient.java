package rs;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import java.nio.file.Path;

public class MyClient {
    private final CountDownLatch num_ReadOK_countdown = new CountDownLatch(3);
    private final CountDownLatch num_Shuffle1OK_countdown = new CountDownLatch(3);
    private final CountDownLatch num_CountOK_countdown = new CountDownLatch(3);
    private final CountDownLatch num_Shuffle2OK_countdown = new CountDownLatch(3);
    private final CountDownLatch num_RequestOK_countdown = new CountDownLatch(3);

    private int[] wordTimesRec = new int[6];
    private int maxOccTimes = -1, minOccTimes = -1;
    private int threshold1 = 0, threshold2 = 0;

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
    
    class SocketClient {
        // Server Host
        public String serverHost = "tp-1a226-01.enst.fr";
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
    }

    class SocketClientThread extends Thread {
        public String serverHost = "";
        public int port = 9681;
        public String name = "";
        public int threadID = 0;
        public SocketClientThread (String serverHost, int port, String name, int threadID) {
            this.serverHost = serverHost;
            this.port = port;
            this.name = name;
            this.threadID = threadID;
        }
        public void waitRead ( ) {
            num_ReadOK_countdown.countDown();
            try {
                num_ReadOK_countdown.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        public void waitShuffle1 ( ) {
            num_Shuffle1OK_countdown.countDown();
            try {
                num_Shuffle1OK_countdown.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        public void waitCount ( ) {
            num_CountOK_countdown.countDown();
            try {
                num_CountOK_countdown.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        public void waitShuffle2 ( ) {
            num_Shuffle2OK_countdown.countDown();
            try {
                num_Shuffle2OK_countdown.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        public void waitRequest ( ) {
            num_RequestOK_countdown.countDown();
            try {
                num_RequestOK_countdown.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Calculate the thresholds using Zipf's law
        public double calculateThreshold1 (int x,int y) {
            double logX = Math.log(x);
            double logY = Math.log(y);
            double exponent = (logY - logX) / 3.0;
            return x * Math.exp(exponent);
        }
        public double calculateThreshold2 (double thr,int x,int y) {
            double logX = Math.log(x);
            double logY = Math.log(y);
            double exponent = (logY - logX) / 3.0;
            return thr * Math.exp(exponent);
        }
        public void run ( ) {
            long startTime = 0, endTime = 0;
            SocketClient socketClient = new SocketClient(serverHost, port, name);
            
            startTime = System.currentTimeMillis();

            socketClient.sendMessage("[NUM]"+threadID);
            socketClient.sendMessage("[READ]");
            waitRead();

            endTime = System.currentTimeMillis();
            if (threadID == 1) {
                System.out.println("Map1 Time: " + (endTime - startTime) + "ms");
            }
            System.out.println("Server "+threadID+" finished reading.");

            startTime = System.currentTimeMillis();

            socketClient.sendMessage("[SHUFFLE1]");
            waitShuffle1();

            endTime = System.currentTimeMillis();
            if (threadID == 1) {
                System.out.println("Shuffle1 + Reduce1 Time: " + (endTime - startTime) + "ms");
            }

            System.out.println("Server "+threadID+" finished shuffle1.");

            startTime = System.currentTimeMillis();

            String response = socketClient.sendMessage("[COUNT]");
            String[] responseNum = response.split(";",2);
            int fmin = Integer.parseInt(responseNum[0]), fmax = Integer.parseInt(responseNum[1]);
            wordTimesRec[threadID*2-2] = fmin;
            wordTimesRec[threadID*2-1] = fmax;
            waitCount();
            for (int i=0; i<6; i++) {
                if (maxOccTimes == -1 || wordTimesRec[i] > maxOccTimes) {
                    maxOccTimes = wordTimesRec[i];
                }
                if (minOccTimes == -1 || wordTimesRec[i] < minOccTimes) {
                    minOccTimes = wordTimesRec[i];
                }
            }
            endTime = System.currentTimeMillis();
            if (threadID == 1) {
                System.out.println("Count Time: " + (endTime - startTime) + "ms");
            }
            System.out.println("Server "+threadID+" finished counting.");
            double thrTmp = calculateThreshold1(minOccTimes,maxOccTimes);
            threshold1 = (int) (Math.round(thrTmp));
            threshold2 = (int) (Math.round(calculateThreshold2(thrTmp,minOccTimes,maxOccTimes)));

            startTime = System.currentTimeMillis();
            socketClient.sendMessage("[SHUFFLE2]"+threshold1+";"+threshold2);
            waitShuffle2();
            endTime = System.currentTimeMillis();
            if (threadID == 1) {
                System.out.println("Shuffle2 + Reduce2 Time: " + (endTime - startTime) + "ms");
            }
            System.out.println("Server "+threadID+" finished shuffle2.");

            startTime = System.currentTimeMillis();
            socketClient.sendMessage("[REQUEST]");
            waitRequest();
            endTime = System.currentTimeMillis();
            if (threadID == 1) {
                System.out.println("Request Time: " + (endTime - startTime) + "ms");
            }
            System.out.println("Server "+threadID+" finished requesting.");
        }
    }

    public static void splitFile(String filePath) throws IOException {
        Path inputPath = Paths.get(filePath);
        String fileName = inputPath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        String outputFilePath1 = Paths.get(".", baseName + "_part1" + extension).toString();
        String outputFilePath2 = Paths.get(".", baseName + "_part2" + extension).toString();
        String outputFilePath3 = Paths.get(".", baseName + "_part3" + extension).toString();

        int totalLines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.readLine() != null) {
                totalLines++;
            }
        }

        int partSize = totalLines / 3;
        int remainder = totalLines % 3;

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
            BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath1), "UTF-8"));
            BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath2), "UTF-8"));
            BufferedWriter writer3 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath3), "UTF-8"))
        ) {
            String line;
            int currentLine = 0;

            while ((line = reader.readLine()) != null) {
                if (currentLine < partSize + (remainder > 0 ? 1 : 0)) {
                    writer1.write(line);
                    writer1.newLine();
                } else if (currentLine < partSize * 2 + (remainder > 1 ? 2 : 1)) {
                    writer2.write(line);
                    writer2.newLine();
                } else {
                    writer3.write(line);
                    writer3.newLine();
                }
                currentLine++;
            }
        }
    }

    public static void main(String[] args) {
        long startTime = 0, endTime = 0;
        MyClient myClient = new MyClient();

        String[] server = {"tp-4b01-04", "tp-4b01-06", "tp-4b01-07"};
        String username = "yuanjie";
        String password = "123456";
        int FTPPort = 7927, socketPort = 9681;

        String[] localFilePaths = {"/cal/commoncrawl/CC-MAIN-20230320083513-20230320113513-00000.warc.wet"};
        String[] remoteFilenames = {"test.txt"};
        if (args.length > 0) {
            localFilePaths[0] = args[0];
        }

        startTime = System.currentTimeMillis();
        try{
            splitFile(localFilePaths[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FTPServerConnection myFTPServer1 = myClient.new FTPServerConnection(server[0], FTPPort, username, password);
        FTPServerConnection myFTPServer2 = myClient.new FTPServerConnection(server[1], FTPPort, username, password);
        FTPServerConnection myFTPServer3 = myClient.new FTPServerConnection(server[2], FTPPort, username, password);

        Path inputPath = Paths.get(localFilePaths[0]);
        String fileName = inputPath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        String outputFilePath1 = Paths.get(".", baseName + "_part1" + extension).toString();
        String outputFilePath2 = Paths.get(".", baseName + "_part2" + extension).toString();
        String outputFilePath3 = Paths.get(".", baseName + "_part3" + extension).toString();

        myFTPServer1.sendFileFromLocal(outputFilePath1, remoteFilenames[0]);
        myFTPServer2.sendFileFromLocal(outputFilePath2, remoteFilenames[0]);
        myFTPServer3.sendFileFromLocal(outputFilePath3, remoteFilenames[0]);

        endTime = System.currentTimeMillis();
        System.out.println("Splitting Time: " + (endTime - startTime) + "ms");
        
        //myFTPServer1.sendFileFromLocal(localFilePaths[0], remoteFilenames[0]);
        //myFTPServer2.sendFileFromLocal(localFilePaths[1], remoteFilenames[0]);
        //myFTPServer3.sendFileFromLocal(localFilePaths[2], remoteFilenames[0]);
        

        System.out.println("FTP client finished.");

        SocketClientThread socketClientThread1 = myClient.new SocketClientThread(server[0], socketPort, "N0", 1);
        SocketClientThread socketClientThread2 = myClient.new SocketClientThread(server[1], socketPort, "N0", 2);
        SocketClientThread socketClientThread3 = myClient.new SocketClientThread(server[2], socketPort, "N0", 3);
        socketClientThread1.start();
        socketClientThread2.start();
        socketClientThread3.start();

        try {
            socketClientThread1.join();
            socketClientThread2.join();
            socketClientThread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}