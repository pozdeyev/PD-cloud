package com.geekbrains.cloud.common;
import java.util.ArrayList;

//Класс для передачи списка файлов на сервере
public class FileListServer  extends AbstractMessage{
    private ArrayList<String> fileList;

    //Конструктор
    public FileListServer(ArrayList<String> list) {
        this.fileList = list;
    }

    //Геттер возвращает список файлов
    public ArrayList<String> getFileList() {
        return fileList;
    }
}
