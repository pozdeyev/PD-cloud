package com.geekbrains.cloud.server;

import com.geekbrains.cloud.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ArrayList;


public class MainHandler extends ChannelInboundHandlerAdapter {
    private String user;

    private String pass;
    //Флаг авторизации
    private boolean auth=false;
    private AuthService authService;

    //Создаем экземпляр авторизации
    {
        try {
            authService = new AuthServiceImpl();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }

            if (msg instanceof AuthMessage) {
                AuthMessage authMessage = (AuthMessage) msg;
                user = authMessage.getName();
                pass = authMessage.getPass();

                if (authService.authUser(user, pass)) {
                    System.out.println("Клиент: " + user);
                    System.out.println("Пароль правильный");
                    auth=true;
                    authMessage.setAuth(true);
                    ctx.writeAndFlush(authMessage);
                }

                //Если пользователь вышел
                if (pass.equals("/disconnect-->/")){
                    System.out.println("Клиент: " + user + " отключен");
                    auth=false;
                }
                //Если пользователь не авторизован и не вышел
                if ((authService.authUser(user, pass)==false) && !(pass.equals("/disconnect-->/"))) {
                    System.out.printf("Авторизация %s не прошла \n", user);
                    auth=false;
                    authMessage.setAuth(false);
                    ctx.writeAndFlush(authMessage);
                }
            }


            if (auth && msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                    if (Files.exists(Paths.get(user + "_server_storage/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get(user + "_server_storage/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (auth && msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                if (!Files.exists(Paths.get(user + "_server_storage/" + fm.getFilename()))) {
                    Files.write(Paths.get(user + "_server_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                    ctx.writeAndFlush(getFileList());
                }
            }
            if (auth && msg instanceof FileDelete) {
                FileDelete fd = (FileDelete) msg;
                if (Files.exists(Paths.get(user + "_server_storage/" + fd.getFilename()))) {
                    Files.delete(Paths.get(user + "_server_storage/" + fd.getFilename()));
                    ctx.writeAndFlush(getFileList());
                }
            }
            if (auth && msg instanceof FileListServer) {

                String dirPath =user + "_server_storage/";
                Path dirPathObj = Paths.get(dirPath);
                boolean dirExists = Files.exists(dirPathObj);
                if(dirExists) {
                    System.out.println("Каталог на сервере: " + dirPath + " уже существует");
                } else {
                    try {
                        Files.createDirectories(dirPathObj);
                        System.out.println("Директория " +  dirPath + " создана, так как ее не было на сервере");
                    } catch (IOException ioExceptionObj) {
                        System.out.println("Возникла проблема при создании каталога: " + ioExceptionObj.getMessage());
                    }
                }


                ctx.writeAndFlush(getFileList());
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    public FileListServer getFileList() throws IOException {
        ArrayList<String> arr = new ArrayList<>();
        Files.list(Paths.get(user + "_server_storage/")).map(p -> p.getFileName().toString()).forEach(o -> arr.add(o));
        return new FileListServer(arr);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
