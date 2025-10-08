package com.photowatermark;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class MainController implements Initializable {
    // 界面元素
    @FXML
    private FlowPane imageListContainer;
    @FXML
    private ImageView previewImageView;
    @FXML
    private GridPane watermarkControls;
    @FXML
    private TextField watermarkText;
    @FXML
    private Slider opacitySlider;
    @FXML
    private ChoiceBox<String> fontChoiceBox;
    @FXML
    private Slider fontSizeSlider;
    @FXML
    private CheckBox boldCheckBox;
    @FXML
    private CheckBox italicCheckBox;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private RadioButton textWatermarkRadio;
    @FXML
    private RadioButton imageWatermarkRadio;
    @FXML
    private javafx.scene.control.Button selectWatermarkImageBtn;
    @FXML
    private VBox positionPresets;
    @FXML
    private Slider rotationSlider;
    @FXML
    private TextField exportPrefix;
    @FXML
    private TextField exportSuffix;
    @FXML
    private CheckBox keepOriginalNameCheck;
    @FXML
    private ChoiceBox<String> exportFormatChoice;
    @FXML
    private Slider qualitySlider;
    @FXML
    private Button selectOutputDirBtn;
    @FXML
    private Label outputDirLabel;
    @FXML
    private ListView<String> templateListView;
    @FXML
    private TextField templateNameField;
    @FXML
    private Label fontSizeLabel;
    @FXML
    private Label opacityLabel;
    @FXML
    private Label rotationLabel;
    @FXML
    private Label qualityLabel;
    @FXML
    private VBox textWatermarkControls;
    @FXML
    private Slider watermarkImageSizeSlider;
    @FXML
    private Label watermarkImageSizeLabel;

    // 数据模型
    private ObservableList<ImageItem> imageItems = FXCollections.observableArrayList();
    private ImageItem selectedImageItem = null;
    private File watermarkImageFile = null;
    private Image watermarkImage = null;
    private File outputDirectory = null;
    private Map<String, WatermarkTemplate> templates = new HashMap<>();
    private Preferences preferences = Preferences.userNodeForPackage(MainController.class);
    private double watermarkX = 0;
    private double watermarkY = 0;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double watermarkImageScale = 25.0; // 默认缩放比例为原图的25%

    // 水印类型枚举
    private enum WatermarkType {
        TEXT, IMAGE
    }
    private WatermarkType currentWatermarkType = WatermarkType.TEXT;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 初始化字体选择器
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        fontChoiceBox.setItems(FXCollections.observableArrayList(fontNames));
        if (fontNames.length > 0) {
            fontChoiceBox.setValue(fontNames[0]);
        }

        // 初始化导出格式选择器
        exportFormatChoice.setItems(FXCollections.observableArrayList("PNG", "JPEG"));
        exportFormatChoice.setValue("PNG");

        // 初始化位置预设按钮
        initializePositionPresets();

        // 添加事件监听器
        addEventListeners();

        // 初始化水印图片大小滑块
        if (watermarkImageSizeSlider != null) {
            watermarkImageSizeSlider.setMin(1);
            watermarkImageSizeSlider.setMax(100);
            watermarkImageSizeSlider.setValue(watermarkImageScale);
            watermarkImageSizeLabel.setText(watermarkImageScale + "%");
        }

        // 加载设置和模板
        loadSettings();
        loadTemplates();

        // 启用拖拽功能
        enableDragAndDrop();
    }

    private void initializePositionPresets() {
        // 清空现有的位置预设
        positionPresets.getChildren().clear();
        
        // 创建3x3网格布局的位置预设按钮
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        
        String[] positions = {
                "左上", "上中", "右上",
                "左中", "中心", "右中",
                "左下", "下中", "右下"
        };
        
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Button btn = new Button(positions[index]);
                btn.setMinSize(60, 30);
                btn.setMaxSize(60, 30);
                final int finalRow = row;
                final int finalCol = col;
                btn.setOnAction(event -> setWatermarkPosition(finalRow, finalCol));
                grid.add(btn, col, row);
                index++;
            }
        }
        
        positionPresets.getChildren().add(grid);
    }

    private void setWatermarkPosition(int row, int col) {
        if (selectedImageItem == null || previewImageView.getImage() == null) return;
        
        double imageWidth = previewImageView.getBoundsInLocal().getWidth();
        double imageHeight = previewImageView.getBoundsInLocal().getHeight();
        
        // 计算水印位置
        double x, y;
        
        switch (col) {
            case 0: x = 10; break; // 左
            case 1: x = imageWidth / 2; break; // 中
            case 2: x = imageWidth - 10; break; // 右
            default: x = imageWidth / 2;
        }
        
        switch (row) {
            case 0: y = 10; break; // 上
            case 1: y = imageHeight / 2; break; // 中
            case 2: y = imageHeight - 10; break; // 下
            default: y = imageHeight / 2;
        }
        
        watermarkX = x;
        watermarkY = y;
        
        // 更新预览
        updatePreview();
    }

    private void addEventListeners() {
        // 水印文本变化
        watermarkText.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        
        // 透明度滑块
        opacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            opacityLabel.setText((int) Math.round(newValue.doubleValue()) + "%");
            updatePreview();
        });
        
        // 字体选择
        fontChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        
        // 字体大小滑块
        fontSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            fontSizeLabel.setText(String.valueOf((int) Math.round(newValue.doubleValue())));
            updatePreview();
        });
        
        // 粗体和斜体复选框
        boldCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        italicCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        
        // 颜色选择器
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        
        // 水印类型单选按钮
        ToggleGroup watermarkTypeGroup = new ToggleGroup();
        textWatermarkRadio.setToggleGroup(watermarkTypeGroup);
        imageWatermarkRadio.setToggleGroup(watermarkTypeGroup);
        textWatermarkRadio.setSelected(true);
        
        watermarkTypeGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == textWatermarkRadio) {
                currentWatermarkType = WatermarkType.TEXT;
                enableTextWatermarkControls(true);
                enableImageWatermarkControls(false);
            } else if (newToggle == imageWatermarkRadio) {
                currentWatermarkType = WatermarkType.IMAGE;
                enableTextWatermarkControls(false);
                enableImageWatermarkControls(true);
            }
            updatePreview();
        });
        
        // 旋转滑块
        rotationSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            rotationLabel.setText((int) Math.round(newValue.doubleValue()) + "°");
            updatePreview();
        });
        
        // 水印图片大小滑块
        if (watermarkImageSizeSlider != null) {
            watermarkImageSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                watermarkImageScale = newValue.doubleValue();
                watermarkImageSizeLabel.setText((int) Math.round(watermarkImageScale) + "%");
                updatePreview();
            });
        }
        
        // 质量滑块
        qualitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            qualityLabel.setText((int) Math.round(newValue.doubleValue()) + "%");
            saveSettings();
        });
        
        // 导出设置变化
        exportPrefix.textProperty().addListener((observable, oldValue, newValue) -> saveSettings());
        exportSuffix.textProperty().addListener((observable, oldValue, newValue) -> saveSettings());
        keepOriginalNameCheck.selectedProperty().addListener((observable, oldValue, newValue) -> saveSettings());
        exportFormatChoice.valueProperty().addListener((observable, oldValue, newValue) -> saveSettings());
        
        // 预览图鼠标事件用于拖拽水印
        previewImageView.setOnMousePressed(this::handleMousePressed);
        previewImageView.setOnMouseDragged(this::handleMouseDragged);
        previewImageView.setOnMouseReleased(this::handleMouseReleased);
    }

    private void enableTextWatermarkControls(boolean enable) {
        watermarkText.setDisable(!enable);
        fontChoiceBox.setDisable(!enable);
        fontSizeSlider.setDisable(!enable);
        boldCheckBox.setDisable(!enable);
        italicCheckBox.setDisable(!enable);
        colorPicker.setDisable(!enable);
    }

    private void enableImageWatermarkControls(boolean enable) {
        selectWatermarkImageBtn.setDisable(!enable);
        if (watermarkImageSizeSlider != null) {
            watermarkImageSizeSlider.setDisable(!enable);
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (selectedImageItem == null || previewImageView.getImage() == null) return;
        
        // 检查点击位置是否在水印上（简化处理，实际应用中需要更精确的检测）
        double clickX = event.getX();
        double clickY = event.getY();
        
        // 如果点击位置接近水印位置，开始拖拽
        if (Math.abs(clickX - watermarkX) < 50 && Math.abs(clickY - watermarkY) < 50) {
            dragStartX = clickX - watermarkX;
            dragStartY = clickY - watermarkY;
            previewImageView.setCursor(Cursor.MOVE);
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (selectedImageItem == null || previewImageView.getImage() == null) return;
        
        double newX = event.getX() - dragStartX;
        double newY = event.getY() - dragStartY;
        
        // 限制水印在图片范围内
        double imageWidth = previewImageView.getBoundsInLocal().getWidth();
        double imageHeight = previewImageView.getBoundsInLocal().getHeight();
        
        newX = Math.max(0, Math.min(imageWidth, newX));
        newY = Math.max(0, Math.min(imageHeight, newY));
        
        watermarkX = newX;
        watermarkY = newY;
        
        updatePreview();
    }

    private void handleMouseReleased(MouseEvent event) {
        previewImageView.setCursor(Cursor.DEFAULT);
    }

    private void enableDragAndDrop() {
        // 为图片列表容器添加拖拽功能
        imageListContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != imageListContainer &&
                    event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        
        imageListContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles()) {
                success = true;
                List<File> files = db.getFiles();
                importFiles(files);
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @FXML
    private void handleImportImages(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        
        // 设置文件过滤器
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "图片文件", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.tiff");
        fileChooser.getExtensionFilters().add(imageFilter);
        
        // 显示文件选择对话框
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            importFiles(selectedFiles);
        }
    }

    @FXML
    private void handleImportFolder(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择文件夹");
        
        File selectedDir = directoryChooser.showDialog(null);
        
        if (selectedDir != null && selectedDir.isDirectory()) {
            List<File> imageFiles = new ArrayList<>();
            scanDirectoryForImages(selectedDir, imageFiles);
            
            if (!imageFiles.isEmpty()) {
                importFiles(imageFiles);
            }
        }
    }

    private void scanDirectoryForImages(File directory, List<File> imageFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectoryForImages(file, imageFiles);
                } else if (isImageFile(file)) {
                    imageFiles.add(file);
                }
            }
        }
    }

    private boolean isImageFile(File file) {
        String extension = getFileExtension(file).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("bmp") || 
               extension.equals("tiff");
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return name.substring(lastDotIndex + 1);
        }
        return "";
    }

    private void importFiles(List<File> files) {
        for (File file : files) {
            if (isImageFile(file)) {
                try {
                    Image image = new Image(file.toURI().toString());
                    ImageItem imageItem = new ImageItem(file, image);
                    imageItems.add(imageItem);
                    addImageToUI(imageItem);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "导入失败", "无法导入图片: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        
        // 如果这是第一次导入图片，自动选择第一张
        if (!imageItems.isEmpty() && selectedImageItem == null) {
            selectImageItem(imageItems.get(0));
        }
    }

    private void addImageToUI(ImageItem imageItem) {
        // 创建图片预览项
        VBox imagePreview = new VBox();
        imagePreview.setPadding(new Insets(5));
        imagePreview.setAlignment(Pos.CENTER);
        
        // 创建缩略图
        ImageView thumbnail = new ImageView(imageItem.getImage());
        thumbnail.setFitWidth(100);
        thumbnail.setFitHeight(100);
        thumbnail.setPreserveRatio(true);
        
        // 创建文件名标签
        Label fileNameLabel = new Label(imageItem.getFile().getName());
        fileNameLabel.setMaxWidth(100);
        fileNameLabel.setWrapText(true);
        fileNameLabel.setAlignment(Pos.CENTER);
        
        // 添加到容器
        imagePreview.getChildren().addAll(thumbnail, fileNameLabel);
        
        // 添加点击事件
        imagePreview.setOnMouseClicked(event -> selectImageItem(imageItem));
        
        // 添加到图片列表
        imageListContainer.getChildren().add(imagePreview);
    }

    private void selectImageItem(ImageItem imageItem) {
        // 取消之前选择的图片的高亮状态
        if (selectedImageItem != null) {
            for (int i = 0; i < imageListContainer.getChildren().size(); i++) {
                Node node = imageListContainer.getChildren().get(i);
                if (node instanceof VBox && i == imageItems.indexOf(selectedImageItem)) {
                    node.setStyle("");
                    break;
                }
            }
        }
        
        // 设置新的选中项
        selectedImageItem = imageItem;
        
        // 高亮显示选中的图片
        for (int i = 0; i < imageListContainer.getChildren().size(); i++) {
            Node node = imageListContainer.getChildren().get(i);
            if (node instanceof VBox && i == imageItems.indexOf(selectedImageItem)) {
                node.setStyle("-fx-border-color: blue; -fx-border-width: 2px;");
                break;
            }
        }
        
        // 更新预览
        updatePreview();
    }

    private void updatePreview() {
        if (selectedImageItem == null) return;
        
        try {
            // 创建带水印的预览图片
            BufferedImage originalImage = ImageIO.read(selectedImageItem.getFile());
            BufferedImage previewImage = addWatermark(originalImage);
            
            // 转换为JavaFX Image并显示
            Image fxImage = SwingFXUtils.toFXImage(previewImage, null);
            previewImageView.setImage(fxImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage addWatermark(BufferedImage originalImage) {
        // 创建一个可绘制的副本
        BufferedImage watermarkedImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = watermarkedImage.createGraphics();
        
        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);
        
        // 设置透明度
        float opacity = (float) (opacitySlider.getValue() / 100.0);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        
        // 应用旋转
        double rotation = Math.toRadians(rotationSlider.getValue());
        
        if (currentWatermarkType == WatermarkType.TEXT) {
            // 添加文本水印
            String text = watermarkText.getText();
            if (!text.isEmpty()) {
                // 设置字体
                String fontName = fontChoiceBox.getValue();
                int fontSize = (int) fontSizeSlider.getValue();
                int fontStyle = java.awt.Font.PLAIN;
                if (boldCheckBox.isSelected()) fontStyle |= java.awt.Font.BOLD;
                if (italicCheckBox.isSelected()) fontStyle |= java.awt.Font.ITALIC;
                
                g2d.setFont(new java.awt.Font(fontName, fontStyle, fontSize));
                
                // 设置颜色
                Color color = colorPicker.getValue();
                g2d.setColor(new java.awt.Color(
                        (float) color.getRed(), 
                        (float) color.getGreen(), 
                        (float) color.getBlue()));
                
                // 获取文本边界
                FontMetrics metrics = g2d.getFontMetrics();
                int textWidth = metrics.stringWidth(text);
                int textHeight = metrics.getHeight();
                
                // 计算实际位置（考虑文本中心）
                double actualX = watermarkX - textWidth / 2;
                double actualY = watermarkY + textHeight / 4;
                
                // 应用旋转
                g2d.rotate(rotation, watermarkX, watermarkY);
                
                // 绘制文本
                g2d.drawString(text, (float) actualX, (float) actualY);
                
                // 恢复旋转
                g2d.rotate(-rotation, watermarkX, watermarkY);
            }
        } else if (currentWatermarkType == WatermarkType.IMAGE && watermarkImageFile != null) {
            // 添加图片水印
            try {
                BufferedImage watermarkImg = ImageIO.read(watermarkImageFile);
                
                // 按比例缩放水印图片
                double scaleFactor = watermarkImageScale / 100.0;
                int scaledWidth = (int) (watermarkImg.getWidth() * scaleFactor);
                int scaledHeight = (int) (watermarkImg.getHeight() * scaleFactor);
                
                // 应用旋转
                g2d.rotate(rotation, watermarkX, watermarkY);
                
                // 绘制水印图片
                g2d.drawImage(watermarkImg, 
                        (int) (watermarkX - scaledWidth / 2), 
                        (int) (watermarkY - scaledHeight / 2), 
                        scaledWidth, 
                        scaledHeight, 
                        null);
                
                // 恢复旋转
                g2d.rotate(-rotation, watermarkX, watermarkY);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 释放资源
        g2d.dispose();
        
        return watermarkedImage;
    }

    @FXML
    private void handleSelectWatermarkImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择水印图片");
        
        // 设置文件过滤器
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "图片文件 (*.png, *.jpg, *.jpeg, *.gif, *.bmp)", 
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imageFilter);
        
        // 显示文件选择器
        File selectedFile = fileChooser.showOpenDialog(previewImageView.getScene().getWindow());
        
        if (selectedFile != null) {
            watermarkImageFile = selectedFile;
            watermarkImage = new Image(selectedFile.toURI().toString());
            
            // 如果当前不是图片水印模式，切换到图片水印模式
            if (currentWatermarkType != WatermarkType.IMAGE) {
                imageWatermarkRadio.setSelected(true);
                currentWatermarkType = WatermarkType.IMAGE;
                enableTextWatermarkControls(false);
                enableImageWatermarkControls(true);
            }
            
            // 更新预览
            updatePreview();
            
            // 保存设置
            saveSettings();
        }
    }

    @FXML
    private void handleSelectOutputDir(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择输出文件夹");
        
        if (outputDirectory != null) {
            directoryChooser.setInitialDirectory(outputDirectory);
        }
        
        File selectedDir = directoryChooser.showDialog(null);
        if (selectedDir != null) {
            outputDirectory = selectedDir;
            outputDirLabel.setText(selectedDir.getAbsolutePath());
            saveSettings();
        }
    }

    @FXML
    private void handleExportImages(ActionEvent event) {
        if (imageItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "导出失败", "请先导入图片");
            return;
        }
        
        if (outputDirectory == null) {
            showAlert(Alert.AlertType.WARNING, "导出失败", "请先选择输出文件夹");
            return;
        }
        
        // 创建进度对话框
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        Label progressLabel = new Label("准备导出...");
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("导出图片");
        dialog.setHeaderText("正在导出图片，请稍候...");
        dialog.getDialogPane().setContent(new VBox(10, progressBar, progressLabel));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        // 在单独的线程中执行导出操作
        Thread exportThread = new Thread(() -> {
            try {
                // 获取导出参数
                String prefix = exportPrefix.getText();
                String suffix = exportSuffix.getText();
                boolean keepOriginal = keepOriginalNameCheck.isSelected();
                String format = exportFormatChoice.getValue();
                float quality = (float) (qualitySlider.getValue() / 100.0);
                
                int totalImages = imageItems.size();
                int successCount = 0;
                List<String> failedFiles = new ArrayList<>();
                
                for (int i = 0; i < totalImages; i++) {
                    ImageItem imageItem = imageItems.get(i);
                    try {
                        // 获取原始文件名
                        File originalFile = imageItem.getFile();
                        String originalName = originalFile.getName();
                        String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
                        
                        // 使用generateOutputFileName方法生成新文件名
                        String newFileName = generateOutputFileName(originalFile);
                        
                        // 创建输出文件
                        File outputFile = new File(outputDirectory, newFileName);
                        
                        // 检查输出文件是否在原文件夹中
                        String originalDir = originalFile.getParent();
                        String outputDir = outputFile.getParent();
                        if (originalDir != null && originalDir.equals(outputDir)) {
                            failedFiles.add(originalName + "（为防止覆盖原图，禁止导出到原文件夹）");
                            continue;
                        }
                        
                        // 读取原始图片
                        BufferedImage originalImage = ImageIO.read(originalFile);
                        
                        // 添加水印
                        BufferedImage watermarkedImage = addWatermark(originalImage);
                        
                        // 导出图片
                        if ("PNG".equals(format)) {
                            ImageIO.write(watermarkedImage, "PNG", outputFile);
                        } else if ("JPEG".equals(format)) {
                            // 对于JPEG格式，设置质量
                            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                            ImageWriteParam param = writer.getDefaultWriteParam();
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(quality);
                            
                            try (FileImageOutputStream output = new FileImageOutputStream(outputFile)) {
                                writer.setOutput(output);
                                IIOImage image = new IIOImage(watermarkedImage, null, null);
                                writer.write(null, image, param);
                                writer.dispose();
                            }
                        }
                        
                        successCount++;
                    } catch (IOException e) {
                        failedFiles.add(imageItem.getFile().getName() + "（" + e.getMessage() + "）");
                        e.printStackTrace();
                    }
                    
                    // 更新进度
                    final int progress = i + 1;
                    Platform.runLater(() -> {
                        progressBar.setProgress((double) progress / totalImages);
                        progressLabel.setText("已完成 " + progress + " / " + totalImages + " 张图片");
                    });
                }
                
                // 导出完成后显示结果
                final File finalOutputDirectory = outputDirectory; // 创建final局部变量持有outputDirectory引用
                final Dialog<Void> finalDialog = dialog; // 创建final局部变量持有dialog引用
                final int finalSuccessCount = successCount; // 创建final局部变量持有successCount引用
                final List<String> finalFailedFiles = new ArrayList<>(failedFiles); // 创建final局部变量持有failedFiles的副本
                
                Platform.runLater(() -> {
                    finalDialog.close();
                    
                    StringBuilder message = new StringBuilder();
                    message.append("导出完成！\n");
                    message.append("成功导出: " + finalSuccessCount + " 张图片\n");
                    
                    if (!finalFailedFiles.isEmpty()) {
                        message.append("导出失败: " + finalFailedFiles.size() + " 张图片\n");
                        for (String failedFile : finalFailedFiles) {
                            message.append("  - " + failedFile + "\n");
                        }
                    }
                    
                    message.append("\n输出文件夹: " + finalOutputDirectory.getAbsolutePath());
                    
                    showAlert(Alert.AlertType.INFORMATION, "导出结果", message.toString());
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    dialog.close();
                    showAlert(Alert.AlertType.ERROR, "导出失败", "导出图片时发生错误：" + e.getMessage());
                });
            }
        });
        
        // 设置取消按钮的行为
        dialog.setOnCloseRequest(e -> {
            if (exportThread.isAlive()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认取消");
                confirmAlert.setHeaderText("正在导出图片");
                confirmAlert.setContentText("确定要取消导出吗？");
                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    exportThread.interrupt();
                } else {
                    e.consume(); // 取消关闭对话框
                }
            }
        });
        
        exportThread.start();
        dialog.show();
    }

    private String generateOutputFileName(File originalFile) {
        String originalName = originalFile.getName();
        String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
        String extension = getFileExtension(originalFile);
        
        StringBuilder outputName = new StringBuilder();
        
        // 添加前缀
        if (!exportPrefix.getText().isEmpty()) {
            outputName.append(exportPrefix.getText());
        }
        
        // 添加原始文件名
        if (keepOriginalNameCheck.isSelected()) {
            outputName.append(baseName);
        } else {
            // 生成时间戳作为文件名
            outputName.append(System.currentTimeMillis());
        }
        
        // 添加后缀
        if (!exportSuffix.getText().isEmpty()) {
            outputName.append(exportSuffix.getText());
        }
        
        // 添加扩展名（使用选择的导出格式）
        outputName.append(".").append(exportFormatChoice.getValue().toLowerCase());
        
        return outputName.toString();
    }

    @FXML
    private void handleSaveTemplate(ActionEvent event) {
        String templateName = templateNameField.getText().trim();
        if (templateName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "保存模板失败", "请输入模板名称");
            return;
        }
        
        // 创建并保存模板
        WatermarkTemplate template = new WatermarkTemplate();
        template.setName(templateName);
        template.setWatermarkType(currentWatermarkType);
        template.setText(watermarkText.getText());
        template.setFontName(fontChoiceBox.getValue());
        template.setFontSize(fontSizeSlider.getValue());
        template.setBold(boldCheckBox.isSelected());
        template.setItalic(italicCheckBox.isSelected());
        template.setColor(colorPicker.getValue());
        template.setOpacity(opacitySlider.getValue());
        template.setRotation(rotationSlider.getValue());
        template.setWatermarkImageScale(watermarkImageScale);
        if (watermarkImageFile != null) {
            template.setWatermarkImagePath(watermarkImageFile.getAbsolutePath());
        }
        
        templates.put(templateName, template);
        
        // 更新模板列表
        updateTemplateListView();
        
        // 保存模板到文件
        saveTemplates();
        
        // 清空模板名称输入框
        templateNameField.clear();
        
        showAlert(Alert.AlertType.INFORMATION, "保存成功", "模板已保存");
    }

    @FXML
    private void handleLoadTemplate(ActionEvent event) {
        String selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert(Alert.AlertType.WARNING, "加载模板失败", "请选择要加载的模板");
            return;
        }
        
        WatermarkTemplate template = templates.get(selectedTemplate);
        if (template != null) {
            // 应用模板设置
            currentWatermarkType = template.getWatermarkType();
            textWatermarkRadio.setSelected(currentWatermarkType == WatermarkType.TEXT);
            imageWatermarkRadio.setSelected(currentWatermarkType == WatermarkType.IMAGE);
            
            watermarkText.setText(template.getText());
            fontChoiceBox.setValue(template.getFontName());
            fontSizeSlider.setValue(template.getFontSize());
            boldCheckBox.setSelected(template.isBold());
            italicCheckBox.setSelected(template.isItalic());
            colorPicker.setValue(template.getColor());
            opacitySlider.setValue(template.getOpacity());
            rotationSlider.setValue(template.getRotation());
            watermarkImageScale = template.getWatermarkImageScale();
            
            // 更新水印图片大小滑块
            if (watermarkImageSizeSlider != null) {
                watermarkImageSizeSlider.setValue(watermarkImageScale);
                watermarkImageSizeLabel.setText((int) Math.round(watermarkImageScale) + "%");
            }
            
            // 加载水印图片
            if (template.getWatermarkImagePath() != null) {
                File watermarkFile = new File(template.getWatermarkImagePath());
                if (watermarkFile.exists()) {
                    watermarkImageFile = watermarkFile;
                    watermarkImage = new Image(watermarkFile.toURI().toString());
                }
            }
            
            // 启用相应的控件
            enableTextWatermarkControls(currentWatermarkType == WatermarkType.TEXT);
            enableImageWatermarkControls(currentWatermarkType == WatermarkType.IMAGE);
            
            // 更新预览
            updatePreview();
        }
    }

    @FXML
    private void handleDeleteTemplate(ActionEvent event) {
        String selectedTemplate = templateListView.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null) {
            showAlert(Alert.AlertType.WARNING, "删除模板失败", "请选择要删除的模板");
            return;
        }
        
        // 确认删除
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除模板 \"" + selectedTemplate + "\" 吗？");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 删除模板
            templates.remove(selectedTemplate);
            
            // 更新模板列表
            updateTemplateListView();
            
            // 保存模板到文件
            saveTemplates();
        }
    }

    private void updateTemplateListView() {
        ObservableList<String> templateNames = FXCollections.observableArrayList(templates.keySet());
        templateListView.setItems(templateNames);
    }

    private void saveTemplates() {
        try {
            // 这里简化处理，实际应用中应该使用更可靠的序列化方法
            StringBuilder templateData = new StringBuilder();
            for (WatermarkTemplate template : templates.values()) {
                templateData.append(template.serialize()).append("\n");
            }
            preferences.put("templates", templateData.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTemplates() {
        try {
            String templateData = preferences.get("templates", "");
            if (!templateData.isEmpty()) {
                String[] templateLines = templateData.split("\n");
                for (String line : templateLines) {
                    if (!line.isEmpty()) {
                        WatermarkTemplate template = WatermarkTemplate.deserialize(line);
                        if (template != null) {
                            templates.put(template.getName(), template);
                        }
                    }
                }
            }
            
            // 更新模板列表
            updateTemplateListView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSettings() {
        try {
            // 保存水印设置
            preferences.put("watermarkText", watermarkText.getText());
            preferences.putDouble("opacity", opacitySlider.getValue());
            preferences.put("fontName", fontChoiceBox.getValue());
            preferences.putDouble("fontSize", fontSizeSlider.getValue());
            preferences.putBoolean("bold", boldCheckBox.isSelected());
            preferences.putBoolean("italic", italicCheckBox.isSelected());
            preferences.put("color", colorPicker.getValue().toString());
            preferences.put("watermarkType", currentWatermarkType.toString());
            if (watermarkImageFile != null) {
                preferences.put("watermarkImagePath", watermarkImageFile.getAbsolutePath());
            }
            preferences.putDouble("rotation", rotationSlider.getValue());
            preferences.putDouble("watermarkX", watermarkX);
            preferences.putDouble("watermarkY", watermarkY);
            preferences.putDouble("watermarkImageScale", watermarkImageScale);
            
            // 保存导出设置
            preferences.put("exportPrefix", exportPrefix.getText());
            preferences.put("exportSuffix", exportSuffix.getText());
            preferences.putBoolean("keepOriginalName", keepOriginalNameCheck.isSelected());
            preferences.put("exportFormat", exportFormatChoice.getValue());
            preferences.putDouble("quality", qualitySlider.getValue());
            if (outputDirectory != null) {
                preferences.put("outputDirectory", outputDirectory.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        try {
            // 加载水印设置
            watermarkText.setText(preferences.get("watermarkText", "水印"));
            opacitySlider.setValue(preferences.getDouble("opacity", 50));
            
            String fontName = preferences.get("fontName", "");
            if (!fontName.isEmpty()) {
                fontChoiceBox.setValue(fontName);
            }
            
            fontSizeSlider.setValue(preferences.getDouble("fontSize", 24));
            boldCheckBox.setSelected(preferences.getBoolean("bold", false));
            italicCheckBox.setSelected(preferences.getBoolean("italic", false));
            
            // 只在colorPicker已经初始化时才尝试设置值
            if (colorPicker != null) {
                String colorStr = preferences.get("color", "#000000");
                try {
                    colorPicker.setValue(Color.web(colorStr));
                } catch (Exception e) {
                    // 如果颜色格式不正确，使用默认颜色
                    colorPicker.setValue(Color.web("#000000"));
                }
            }
            
            String watermarkTypeStr = preferences.get("watermarkType", "TEXT");
            currentWatermarkType = WatermarkType.valueOf(watermarkTypeStr);
            textWatermarkRadio.setSelected(currentWatermarkType == WatermarkType.TEXT);
            imageWatermarkRadio.setSelected(currentWatermarkType == WatermarkType.IMAGE);
            
            String watermarkImagePath = preferences.get("watermarkImagePath", "");
            if (!watermarkImagePath.isEmpty()) {
                File watermarkFile = new File(watermarkImagePath);
                if (watermarkFile.exists()) {
                    watermarkImageFile = watermarkFile;
                    watermarkImage = new Image(watermarkFile.toURI().toString());
                }
            }
            
            rotationSlider.setValue(preferences.getDouble("rotation", 0));
            watermarkX = preferences.getDouble("watermarkX", 0);
            watermarkY = preferences.getDouble("watermarkY", 0);
            watermarkImageScale = preferences.getDouble("watermarkImageScale", 25.0);
            
            // 更新水印图片大小滑块
            if (watermarkImageSizeSlider != null) {
                watermarkImageSizeSlider.setValue(watermarkImageScale);
                watermarkImageSizeLabel.setText((int) Math.round(watermarkImageScale) + "%");
            }
            
            // 加载导出设置
            exportPrefix.setText(preferences.get("exportPrefix", "wm_"));
            exportSuffix.setText(preferences.get("exportSuffix", "_watermarked"));
            keepOriginalNameCheck.setSelected(preferences.getBoolean("keepOriginalName", true));
            
            String exportFormat = preferences.get("exportFormat", "PNG");
            exportFormatChoice.setValue(exportFormat);
            
            qualitySlider.setValue(preferences.getDouble("quality", 90));
            
            String outputDirPath = preferences.get("outputDirectory", "");
            if (!outputDirPath.isEmpty()) {
                File outputDir = new File(outputDirPath);
                if (outputDir.exists() && outputDir.isDirectory()) {
                    outputDirectory = outputDir;
                    outputDirLabel.setText(outputDirPath);
                }
            }
            
            // 启用相应的控件
            enableTextWatermarkControls(currentWatermarkType == WatermarkType.TEXT);
            enableImageWatermarkControls(currentWatermarkType == WatermarkType.IMAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 内部类：图片项
    private class ImageItem {
        private File file;
        private Image image;
        
        public ImageItem(File file, Image image) {
            this.file = file;
            this.image = image;
        }
        
        public File getFile() {
            return file;
        }
        
        public Image getImage() {
            return image;
        }
    }

    // 静态内部类：水印模板
    private static class WatermarkTemplate {
        private String name;
        private WatermarkType watermarkType;
        private String text;
        private String fontName;
        private double fontSize;
        private boolean bold;
        private boolean italic;
        private Color color;
        private double opacity;
        private double rotation;
        private String watermarkImagePath;
        private double watermarkImageScale; // 水印图片缩放比例
        
        // getter和setter方法
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public WatermarkType getWatermarkType() { return watermarkType; }
        public void setWatermarkType(WatermarkType watermarkType) { this.watermarkType = watermarkType; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getFontName() { return fontName; }
        public void setFontName(String fontName) { this.fontName = fontName; }
        
        public double getFontSize() { return fontSize; }
        public void setFontSize(double fontSize) { this.fontSize = fontSize; }
        
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        
        public boolean isItalic() { return italic; }
        public void setItalic(boolean italic) { this.italic = italic; }
        
        public Color getColor() { return color; }
        public void setColor(Color color) { this.color = color; }
        
        public double getOpacity() { return opacity; }
        public void setOpacity(double opacity) { this.opacity = opacity; }
        
        public double getRotation() { return rotation; }
        public void setRotation(double rotation) { this.rotation = rotation; }
        
        public String getWatermarkImagePath() { return watermarkImagePath; }
        public void setWatermarkImagePath(String watermarkImagePath) { this.watermarkImagePath = watermarkImagePath; }
        
        public double getWatermarkImageScale() { return watermarkImageScale; }
        public void setWatermarkImageScale(double watermarkImageScale) { this.watermarkImageScale = watermarkImageScale; }
        
        // 序列化方法（简化版）
        public String serialize() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("|");
            sb.append(watermarkType).append("|");
            sb.append(text).append("|");
            sb.append(fontName).append("|");
            sb.append(fontSize).append("|");
            sb.append(bold).append("|");
            sb.append(italic).append("|");
            sb.append(color.toString()).append("|");
            sb.append(opacity).append("|");
            sb.append(rotation).append("|");
            sb.append(watermarkImageScale).append("|");
            sb.append(watermarkImagePath != null ? watermarkImagePath : "");
            return sb.toString();
        }
        
        // 反序列化方法（简化版）
        public static WatermarkTemplate deserialize(String data) {
            try {
                String[] parts = data.split("\\|", -1);
                if (parts.length >= 12) {
                    WatermarkTemplate template = new WatermarkTemplate();
                    template.setName(parts[0]);
                    template.setWatermarkType(WatermarkType.valueOf(parts[1]));
                    template.setText(parts[2]);
                    template.setFontName(parts[3]);
                    template.setFontSize(Double.parseDouble(parts[4]));
                    template.setBold(Boolean.parseBoolean(parts[5]));
                    template.setItalic(Boolean.parseBoolean(parts[6]));
                    template.setColor(Color.web(parts[7]));
                    template.setOpacity(Double.parseDouble(parts[8]));
                    template.setRotation(Double.parseDouble(parts[9]));
                    template.setWatermarkImageScale(Double.parseDouble(parts[10]));
                    if (!parts[11].isEmpty()) {
                        template.setWatermarkImagePath(parts[11]);
                    }
                    return template;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    @FXML
    private void handleExit(ActionEvent event) {
        saveSettings();
        System.exit(0);
    }
}