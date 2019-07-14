package com.geekbrains.cloud.client;

import com.geekbrains.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class Controller implements Initializable {
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
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                    if (am instanceof FileListServer) {
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
        refreshLocalFilesList();
        refreshServerFilesList();
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
        if (Platform.isFxApplicationThread()) {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    filesList.getItems().clear();
                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void refreshServerFilesList() {
        Network.sendMsg(new FileListServer(null));
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) throws IOException {
        if (tfFileNameUpload.getLength() > 0) {
            Network.sendMsg(new FileMessage(Paths.get("client_storage/" + tfFileNameUpload.getText())));
            tfFileNameUpload.clear();
        }  else if(getSelected(filesList) != null) {
            Network.sendMsg(new FileMessage(Paths.get("client_storage/" + getSelected(filesList))));
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
            Files.delete(Paths.get("client_storage/" + tfFileNameUpload.getText()));
            tfFileNameUpload.clear();
        } else if(getSelected(filesList) != null) {
            Files.delete(Paths.get("client_storage/" + getSelected(filesList)));
        }
        refreshLocalFilesList();
    }

}
