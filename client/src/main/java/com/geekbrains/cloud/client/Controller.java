package com.geekbrains.cloud.client;

import com.geekbrains.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private String user;
    private String pass;
    private boolean auth = false;

    @FXML
    HBox hb1,hb2,hb3,hb4,hb10,hb11,hb12,hb13;

    @FXML
    Label labelStatus;

    @FXML
    Label labelAuth;

    @FXML
    VBox rootNode;

    @FXML
    TextField tfLogin;

    @FXML
    PasswordField pfPass;

    @FXML
    TextField tfFileName;

    @FXML
    TextField tfFileNameUpload;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> filesListServer;

    //При клике мышью в список отображаем имя в строке
    @FXML
    public void handleMouseClickServer() {
        tfFileName.setText(filesListServer.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void handleMouseClickLocal() {
        tfFileNameUpload.setText(filesList.getSelectionModel().getSelectedItem());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showAuthorizationInterface();
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();

                    if (am instanceof AuthMessage) {
                        AuthMessage authMessage = (AuthMessage) am;
                        user = authMessage.getName();
                        auth = authMessage.getAuth();
                        if (auth) {
                            System.out.println("Пользователь " + user + " авторизовался");
                            showWorkInterface();


                            //Обертка для того, чтобы избежать ошибки JavaFX
                            Platform.runLater(new Runnable(){
                                @Override
                                public void run() {
                                    labelAuth.setText(user + "  ");
                                }
                            });


                            refreshLocalFilesList();
                            refreshServerFilesList();
                            } else {
                            System.out.println("Неправильный логин или пароль");
                            //Обертка для того, чтобы избежать ошибки JavaFX
                            Platform.runLater(new Runnable(){
                                @Override
                                public void run() {
                                    labelStatus.setText("Неправильный логин или пароль");
                                }
                            });
                        }
                    }

                    if (auth && am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get(user+ "_client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }

                    if (auth && am instanceof FileListServer) {
                        FileListServer serverFiles = (FileListServer) am;
                        if (Platform.isFxApplicationThread()) {
                            filesListServer.getItems().clear();
                            serverFiles.getFileList().forEach(o -> filesListServer.getItems().add(o));
                        } else {
                            Platform.runLater(() -> {
                                filesListServer.getItems().clear();
                                serverFiles.getFileList().forEach(o -> filesListServer.getItems().add(o));
                            });
                        }
                    }
                }

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        filesList.setItems(FXCollections.observableArrayList());
        filesListServer.setItems(FXCollections.observableArrayList());

    }

    private String getSelected(ListView<String> listView) {
        String item = listView.getSelectionModel().getSelectedItem();
        return item;
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfFileName.getText()));
            tfFileName.clear();
        } else if(getSelected(filesListServer) != null) {
            Network.sendMsg(new FileRequest(getSelected(filesListServer)));
        }
    }

    public void refreshLocalFilesList() {

if (auth) {
    String dirPath = user + "_client_storage/";
    Path dirPathObj = Paths.get(dirPath);
    boolean dirExists = Files.exists(dirPathObj);
    if (dirExists) {
        System.out.println("Каталог на локальной машине: " + dirPath + " существует");
    } else {
        try {
            Files.createDirectories(dirPathObj);
            System.out.println("Директория " + dirPath + " создана, так как ее не было на локальной машине");
        } catch (IOException ioExceptionObj) {
            System.out.println("Возникла проблема при создании каталога: " + ioExceptionObj.getMessage());
        }
    }

    if (Platform.isFxApplicationThread()) {
        try {
            filesList.getItems().clear();
            Files.list(Paths.get(user + "_client_storage/")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
        } catch (IOException e) {
            e.printStackTrace();
        }
    } else {
        Platform.runLater(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get(user + "_client_storage/")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
    }


    //Попытка авторизации по логину/паролю
    public void authorizationBtn(ActionEvent actionEvent) {
        user=tfLogin.getText();
        pass=pfPass.getText();
        Network.sendMsg(new AuthMessage(user, pass,false)); //до ответа сервера в посылке флаг авторизации false
    }

    //Выход из авторизации по кнопке
    public void exitToAuth(ActionEvent actionEvent) {

        showAuthorizationInterface();
        auth=false;
        Network.sendMsg(new AuthMessage(user,"/disconnect-->/", auth)); //флаг авторизации в false
        System.out.println("Пользователь " + user + " вышел");

    }



    public void refreshServerFilesList() {
        Network.sendMsg(new FileListServer(null));
    }
    public void pressOnUploadBtn(ActionEvent actionEvent) throws IOException {
        if (tfFileNameUpload.getLength() > 0) {
            Network.sendMsg(new FileMessage(Paths.get(user+"_client_storage/" + tfFileNameUpload.getText())));
            tfFileNameUpload.clear();
        }  else if(getSelected(filesList) != null) {
            Network.sendMsg(new FileMessage(Paths.get(user+ "_client_storage/" + getSelected(filesList))));
        }
    }

    public void pressOnDeleteServerBtn(ActionEvent actionEvent){
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileDelete(tfFileName.getText()));
            tfFileName.clear();
        } else if(getSelected(filesListServer) != null) {
            Network.sendMsg(new FileDelete(getSelected(filesListServer)));
        }
    }

    public void pressOnDeleteLocalBtn(ActionEvent actionEvent) throws IOException {
        if (tfFileNameUpload.getLength() > 0) {
            Files.delete(Paths.get(user+ "_client_storage/" + tfFileNameUpload.getText()));
            tfFileNameUpload.clear();
        } else if(getSelected(filesList) != null) {
            Files.delete(Paths.get(user+ "_client_storage/" + getSelected(filesList)));
        }
        refreshLocalFilesList();
    }

    public void showWorkInterface (){

        labelStatus.setManaged(false);
        hb10.setManaged(false);
        hb11.setManaged(false);
        hb12.setManaged(false);
        hb13.setManaged(false);

        hb10.setVisible(false);
        hb11.setVisible(false);
        hb12.setVisible(false);
        hb13.setVisible(false);

        filesList.setManaged(true);
        filesListServer.setManaged(true);
        filesList.setVisible(true);
        filesListServer.setVisible(true);

        hb1.setManaged(true);
        hb2.setManaged(true);
        hb3.setManaged(true);
        hb4.setManaged(true);
        hb1.setVisible(true);
        hb2.setVisible(true);
        hb3.setVisible(true);
        hb4.setVisible(true);

        tfLogin.setText("");
        tfFileNameUpload.setText("");

    }

    public void showAuthorizationInterface() {

        tfFileName.setText("");
        pfPass.setText("");

        labelStatus.setManaged(true);
        hb10.setManaged(true);
        hb11.setManaged(true);
        hb12.setManaged(true);
        hb13.setManaged(true);


        hb10.setVisible(true);
        hb11.setVisible(true);
        hb12.setVisible(true);
        hb13.setVisible(true);
        hb13.setVisible(true);


        filesList.setManaged(false);
        filesListServer.setManaged(false);
        filesList.setVisible(false);
        filesListServer.setVisible(false);

        hb1.setVisible(false);
        hb2.setVisible(false);
        hb3.setVisible(false);
        hb4.setVisible(false);

        hb1.setManaged(false);
        hb2.setManaged(false);
        hb3.setManaged(false);
        hb4.setManaged(false);

    }


}
