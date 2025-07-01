/*
 * start program and initializes sender and receiver
 * wait until client wants to send a file
 * should also be able to receive a file
 */

 import java.io.File;
 import java.util.Optional;
 
 import javafx.application.Application;
 import javafx.application.Platform;
 import javafx.concurrent.Task;
 import javafx.geometry.Insets;
 import javafx.geometry.Pos;
 import javafx.scene.Node;
 import javafx.scene.Scene;
 import javafx.scene.control.*;
 import javafx.scene.layout.*;
 import javafx.scene.paint.Color;
 import javafx.stage.FileChooser;
 import javafx.stage.Stage;
 
 public class Main extends Application{
     private String userinfo[] = new String[2];
     private String username = "Enter username";
     private Integer packetSize = 1000;
     private String myIp = "localhost";
     private String filePath = "";
     private String receiverIP = "";
     private File chosenFile = null;
     private ProgressBar fileLoadBar;
 
     public static void main(String[] args) {
         launch(args);
     }
 
     public void start(Stage mainStage) {
         int port = 5000;
         logInUser();
 
         // Start receiver
         Receiver receiver = new Receiver(myIp, port, this);
         Thread thread1 = new Thread(receiver);
         thread1.start();
 
         // Initialise GUI
         Platform.runLater(() -> {
            // Create hidden progress bar
             fileLoadBar = new ProgressBar();
             fileLoadBar.setPrefWidth(600);
             fileLoadBar.setPrefHeight(40);
             fileLoadBar.setStyle("-fx-accent: rgb(200, 150, 210)");
             fileLoadBar.setVisible(false);

             // Create GUI areas
             VBox receiveBox = buildReceiveArea(mainStage, fileLoadBar);
             VBox sendBox = buildSendArea(mainStage, receiveBox);
 
             // Put GUI areas together into one Scene
             BorderPane backdropPane = buildBackDrop(sendBox, receiveBox);
             Scene scene = new Scene(backdropPane, 1330, 700);
             mainStage.setTitle("Chillax File Transfer Client");
             mainStage.setScene(scene);
 
             mainStage.show();
         });
     }
 
     private void logInUser(){
         userinfo = promptUserInfo("");
         username = userinfo[0];
         myIp = userinfo[1];
 
         // Handle login errors
         while (true) {
             if (username.isEmpty() || myIp.isEmpty()) {
                 userinfo = promptUserInfo("Field may not be empty.");
             } else {
                 username = userinfo[0];
                 myIp = userinfo[1];
                 return;
             }
             
             username = userinfo[0];
             myIp = userinfo[1];
         }
     }
 
     private String[] promptUserInfo(String errMessage) {
          // Create custom dialog box
         Dialog<String[]> dialog = new Dialog<>();
         dialog.setTitle("Log In");
         dialog.setResizable(false);
 
         // Set appropriate header text
         if (errMessage.equals("Field may not be empty.")) {
             dialog.setHeaderText("Enter a valid port and IP address.");
         } else {
             dialog.setHeaderText("Welcome to the Chillax file transfer server.");
         }
 
         // Create labels for the dialog box
         Label usernameLabel = new Label("Username: ");
         TextField userNameField = new TextField();
         userNameField.setText(username);
         userNameField.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14; -fx-font-color: white");
         GridPane.setHgrow(userNameField, Priority.ALWAYS);
 
         Label iPLabel = new Label("IP Address: ");
         TextField iPTextField = new TextField();
         iPTextField.setText(myIp);
         iPTextField.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14; -fx-font-color: white");
         GridPane.setHgrow(iPTextField, Priority.ALWAYS);
 
         // Create layout
         GridPane grid = new GridPane();
         grid.setHgap(10);
         grid.setVgap(10);
         grid.add(usernameLabel, 0, 0);
         grid.add(userNameField, 1, 0);
         grid.add(iPLabel, 0, 1);
         grid.add(iPTextField, 1, 1);
         dialog.getDialogPane().setContent(grid);
         dialog.getDialogPane().setStyle("-fx-background: rgb(45, 45, 45); -fx-font-size: 14; -fx-font-color: white");
 
         // Add Log In and Cancel buttons
         dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
         dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle("-fx-background-color: rgb(200, 20, 250); -fx-font-size: 14; -fx-text-fill: white");
         dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
         dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-background-color: rgb(200, 20, 250); -fx-font-size: 14; -fx-text-fill: white");
 
 
         // Set action for OK button
         dialog.setResultConverter(dialogButton -> {
             if (dialogButton == ButtonType.OK) {
                     return new String[] { userNameField.getText(), iPTextField.getText() };
             } else return null;
         });
 
         // Show dialog and wait for result
         Optional<String[]> result = dialog.showAndWait();
         return result.orElseGet(() -> {
             Platform.exit();
             System.exit(0);
             return null;
         });
     }
 
     private VBox buildSendArea(Stage mainStage, VBox receiveBox) {
         // Empty Output area to display file selection later
         TextArea chooseArea = new TextArea();
         chooseArea.setPrefHeight(600);
         chooseArea.setPrefWidth(700);
         chooseArea.setEditable(false);
         chooseArea.setWrapText(true);
         chooseArea.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14");
 
         // Input area for packet size
         Label packetSizeLabel = new Label("Packet Size:");
         packetSizeLabel.setPrefHeight(50);
         packetSizeLabel.setPrefWidth(100);
         packetSizeLabel.setStyle("-fx-font-size: 15; -fx-text-fill: white; -fx-font-weight: bold");
 
         TextField packetSizeField = new TextField("1000");
         packetSizeField.setPrefHeight(50);
         packetSizeField.setPrefWidth(100);
         packetSizeField.setOnAction(e -> changePacketSize(packetSizeField, chooseArea));
         packetSizeField.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14; -fx-text-fill: white");
 
         HBox packetSizeBox = new HBox(packetSizeLabel, packetSizeField);
         packetSizeBox.setStyle("-fx-background-color: rgb(45, 45, 45)");
         
         
         // Options for file transfer
         Label protLabel = new Label("Select Protocol:");
         protLabel.setPrefHeight(50);
         protLabel.setPrefWidth(150);
         protLabel.setPadding(new Insets(0, 0, 0, 10));
         protLabel.setStyle("-fx-font-size: 15; -fx-text-fill: white; -fx-font-weight: bold");
 
         RadioButton tcpButton = new RadioButton("TCP");
         tcpButton.setUserData("TCP");
         tcpButton.setPrefHeight(50);
         tcpButton.setPrefWidth(150);
         tcpButton.setStyle("-fx-text-fill: white");
         
         RadioButton rbudpButton = new RadioButton("RBUDP");
         rbudpButton.setUserData("RBUDP");
         rbudpButton.setPrefHeight(50);
         rbudpButton.setPrefWidth(150);
         rbudpButton.setStyle("-fx-text-fill: white");
 
         ToggleGroup protocolGroup = new ToggleGroup();
         
         tcpButton.setToggleGroup(protocolGroup);
         rbudpButton.setToggleGroup(protocolGroup);
         tcpButton.setSelected(true);
 
         HBox protocolBox = new HBox(protLabel, tcpButton, rbudpButton);
         protocolBox.setSpacing(20);
         protocolBox.setBorder(new Border(new BorderStroke(
             Color.WHITE, 
             BorderStrokeStyle.SOLID, 
             new CornerRadii(10),
             BorderWidths.DEFAULT
         )));
         protocolBox.setStyle("-fx-background-color: rgb(45, 45, 45); -fx-font-size: 14");
 
         HBox optionsBox = new HBox(packetSizeBox, protocolBox);
         optionsBox.setSpacing(10);
 
 
         // Label and text field for receiver IP
         Label receiverIPLabel = new Label("Receiver IP:");
         receiverIPLabel.setPrefHeight(50);
         receiverIPLabel.setPrefWidth(100);
         receiverIPLabel.setStyle("-fx-font-size: 15; -fx-text-fill: white; -fx-font-weight: bold");
 
         TextField receiverIPField = new TextField();
         receiverIPField.setPrefHeight(50);
         receiverIPField.setPrefWidth(100);
         receiverIPField.setOnAction(e -> changeReceiverIP(receiverIPField, chooseArea));
         receiverIPField.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14; -fx-text-fill: white");
 
         HBox receiverIPBox = new HBox(receiverIPLabel, receiverIPField);
         receiverIPBox.setStyle("-fx-background-color: rgb(45, 45, 45)");
 
 
 
         // Button to choose a file and a button to send the file
         Button chooseFileButton = new Button("Choose File");
         chooseFileButton.setPrefWidth(150);
         chooseFileButton.setPrefHeight(40);
         chooseFileButton.setOnAction(e -> chosenFile = chooseFile(mainStage, chooseArea));
         chooseFileButton.setStyle("-fx-background-color: rgb(200, 20, 250); -fx-font-size: 15; -fx-text-fill: white; -fx-font-weight: bold");
         chooseFileButton.setOnMousePressed(e -> chooseFileButton.setStyle("-fx-background-color: rgb(195, 10, 245); -fx-scale-x: 0.95; -fx-scale-y: 0.95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15"));
         chooseFileButton.setOnMouseReleased(e -> chooseFileButton.setStyle("-fx-background-color: rgb(200, 20, 250); -fx-scale-x: 1.0; -fx-scale-y: 1.0; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15"));
 
         Button sendFileButton = new Button("Send File");
         sendFileButton.setPrefWidth(150);
         sendFileButton.setPrefHeight(40);
         if (!(filePath.equals(""))) {
             chooseArea.appendText("No File selected for transfer, please select a file to send.\n");
         } else {
             sendFileButton.setOnAction(e -> sendFile(receiveBox, chosenFile, protocolGroup));
         }
         sendFileButton.setStyle("-fx-background-color: rgb(200, 20, 250); -fx-font-size: 15; -fx-text-fill: white; -fx-font-weight: bold");
         sendFileButton.setOnMousePressed(e -> sendFileButton.setStyle("-fx-background-color: rgb(195, 10, 245); -fx-scale-x: 0.95; -fx-scale-y: 0.95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15"));
         sendFileButton.setOnMouseReleased(e -> sendFileButton.setStyle("-fx-background-color: rgb(200, 20, 250); -fx-scale-x: 1.0; -fx-scale-y: 1.0; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15"));
 
         // Spaces between the buttons
         Region leftSpace = new Region();
         Region midSpace = new Region();
         Region rightSpace = new Region();
 
         HBox.setHgrow(leftSpace, Priority.ALWAYS);
         HBox.setHgrow(midSpace, Priority.ALWAYS);
         HBox.setHgrow(rightSpace, Priority.ALWAYS);
 
         HBox fileBox = new HBox(leftSpace, chooseFileButton, midSpace, sendFileButton, rightSpace);
         fileBox.setStyle("-fx-background-color: rgb(45, 45, 45)");
 
         HBox.setHgrow(fileBox, Priority.ALWAYS);
 
         // Combine all the components
         HBox fileOptionsBox = new HBox(receiverIPBox, fileBox);
         fileOptionsBox.setSpacing(10);
         VBox sendArea = new VBox(chooseArea, optionsBox, fileOptionsBox);
         sendArea.setSpacing(5);
         sendArea.setPadding(new Insets(5, 5, 5, 5));
         return sendArea;
     }
 
     private VBox buildReceiveArea(Stage mainStage, ProgressBar fileLoadBar) {
        // Output area to display to when a new file arrived
        TextArea receiveArea = new TextArea();
        receiveArea.setPrefWidth(600);
        receiveArea.setEditable(false);
        receiveArea.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 15");
        
        // Button to exit the program
        Button exitButton = new Button("Disconnect");
        exitButton.setPrefHeight(40);
        exitButton.setPrefWidth(150);
        exitButton.setStyle("-fx-background-color: rgb(240, 25, 90); -fx-font-size: 15; -fx-text-fill: white; -fx-font-weight: bold");
        exitButton.setOnMousePressed(e -> exitButton.setStyle("-fx-background-color: rgb(200, 20, 70); -fx-scale-x: 0.95; -fx-scale-y: 0.95; text-fill: white"));
        exitButton.setOnMouseReleased(e -> exitButton.setStyle("-fx-background-color: rgb(240, 25, 90); -fx-scale-x: 1.0; -fx-scale-y: 1.0; text-fill: white"));
        exitButton.setOnAction(e -> closeGUI());

        HBox exitBox = new HBox(exitButton);
        exitBox.setAlignment(Pos.CENTER);
        exitBox.setPadding(new Insets(30, 0, 30, 0));

        VBox.setVgrow(receiveArea, Priority.ALWAYS);
        VBox receiveBox = new VBox(receiveArea, exitBox, fileLoadBar);
        receiveBox.setPadding(new Insets(5, 5, 5, 0));

        return receiveBox;
     }
 
     private BorderPane buildBackDrop(VBox sendBox, VBox receiveBox) {
         BorderPane backDropPane = new BorderPane();
         backDropPane.setStyle("-fx-background-color: rgb(45, 45, 45)");
         backDropPane.setLeft(sendBox);
         backDropPane.setRight(receiveBox);
         return backDropPane;
     }
     private File chooseFile(Stage mainStage, TextArea receiveArea) {
         FileChooser fileChooser = new FileChooser();
         fileChooser.setTitle("Choose a file to send");
         fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
 
         File chosenFile = fileChooser.showOpenDialog(mainStage);
 
         if (chosenFile != null) {
             filePath = chosenFile.getAbsolutePath();
             receiveArea.appendText("User chose the following file for transfer: " + filePath + "\n");
         } else {
             return null;
         }
 
         return chosenFile;
     }
 
     private void sendFile(VBox receiveBox, File chosenFile, ToggleGroup protocolGroup) {
         // Create the sender
 
         if (chosenFile != null) {
             Sender sender = new Sender(receiverIP, chosenFile, packetSize);
             Thread thread2 = new Thread(() -> {
                 // Choose between UDP or TCP from the togglegroup
                 if (protocolGroup.getSelectedToggle().getUserData().equals("RBUDP")) {
                     sender.sendFileUDP();
                 } else {
                     sender.sendFileTCP();
                 }
             });
             thread2.start();
         }
     }
 
    private void closeGUI() {
         Platform.exit();
         System.exit(0);
     }
 
    private void buildProgressBar(VBox receiveBox) {
         // Build a progress bar between the receive output area and the buttons
         ProgressBar fileLoadBar = new ProgressBar();
         fileLoadBar.setPrefWidth(550);
         fileLoadBar.setPrefHeight(40);
         fileLoadBar.setStyle("-fx-accent: rgb(200, 150, 210)");
 
         Label progressLabel = new Label("Packages Received: 0/100000");
         progressLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold");
         StackPane progressPane = new StackPane(fileLoadBar, progressLabel);
         receiveBox.getChildren().add(progressPane);
         receiveBox.setSpacing(5);
 
         Task<Void> task = new Task<Void>() {
             int totalPackages = 100000;
             @Override
             protected Void call() throws Exception {
                 for (int packagesReceived = 0; packagesReceived < totalPackages; packagesReceived += packetSize) {
                     Thread.sleep(100);
                     final double curPercent = (double) packagesReceived / totalPackages;
                     final String packagesReceivedString = String.format("%,d", packagesReceived);
                     Platform.runLater(() -> {
                         fileLoadBar.setProgress(curPercent);
                         progressLabel.setText("Packages Received: " + packagesReceivedString + "/100,000");
                     
                     });
                 }
 
                 Platform.runLater(() -> {
                     fileLoadBar.setProgress(1.0);
                     progressLabel.setText("Packages Received: 100,000/100,000");
                     receiveBox.getChildren().remove(progressPane);
                     for (Node node: receiveBox.getChildren()) {
                         if (node instanceof TextArea) {
                             TextArea textArea = (TextArea) node;
                             textArea.appendText("You received a new file!\n");
                             return;
                         }
                     }
                 });
 
                 return null;
             }
         };
 
         new Thread(task).start();
     }
     
    private void changePacketSize(TextField packetSizeField, TextArea chooseArea) {
        packetSize = Integer.parseInt(packetSizeField.getText());
        chooseArea.appendText("New Packet Size: " + packetSize + "\n");
    }
 
    private void changeReceiverIP(TextField receiverIPField, TextArea chooseArea) {
        receiverIP = receiverIPField.getText();
        chooseArea.appendText("Set Receiver IP to: " + receiverIP + "\n");
    }

    public void updateProgressBar(int percent, boolean isSending) {
        Platform.runLater(() -> {
            if (isSending) {
                fileLoadBar.setVisible(true);
                fileLoadBar.setProgress(percent / 100.0);
            } else {
                fileLoadBar.setVisible(false);
            }
        });
    }

}

 