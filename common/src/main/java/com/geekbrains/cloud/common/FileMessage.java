package com.geekbrains.cloud.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;

    public FileMessage(Path path) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }

    //Возвращаем имя
    public String getFilename() {
        return filename;
    }

    //Возвращаем содержимое
    public byte[] getData() {
        return data;
    }

}
