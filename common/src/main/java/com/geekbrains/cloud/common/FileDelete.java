package com.geekbrains.cloud.common;

public class FileDelete extends AbstractMessage {

    private String filename;

//Конструктор состоит из имени файла
    public FileDelete(String filename) {
        this.filename = filename;
    }

//Геттер возвращает имя файла
    public String getFilename() {
        return filename;
    }

}
