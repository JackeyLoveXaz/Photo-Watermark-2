package com.photowatermark;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 加载主界面FXML文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            
            // 设置场景和舞台
            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setTitle("图片水印工具");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            // 设置应用程序关闭时的行为
            primaryStage.setOnCloseRequest(event -> {
                // 在关闭前保存设置
                MainController controller = loader.getController();
                controller.saveSettings();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}