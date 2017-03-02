package view;

import app.Main;
import javafx.application.Platform;
import model.Stream;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

import static app.Main.releaseDate;
import static java.lang.Thread.sleep;


public class Window {

    public DetailTab detailTab;
    public GraphTab graphTab;
    public VisualizationTab visualizationTab;
    public Stage primaryStage;
    Stage aboutStage;
    GridPane gridPane;
    public FileChooser fileChooser;
    public Button selectFileButton;
    public Scene scene;
    StackPane rootPane;
    BorderPane borderPane;
    public ProgressForm progressWindow;
    Alert alertBox;
    VBox topContainer;
    MenuBar mainMenu;
    ToolBar toolBar;
    TabPane tabPane;
    Menu file, edit, help;
    public MenuItem openFile;
    MenuItem importXML;
    MenuItem exportXML;
    MenuItem settings;
    public MenuItem exitApp;
    MenuItem jumpToPacket;
    MenuItem searchPacket;
    public MenuItem about;
    MenuItem manual;
    public TreeItem<String> nodes;

    private Task task;

    public Window(Stage primaryStage) {

        this.task = null;

        this.primaryStage = primaryStage;
        aboutStage = new Stage();

        rootPane = new StackPane();
        gridPane = new GridPane();
        borderPane = new BorderPane();

        topContainer = new VBox();  //Creates a container to hold all Menu Objects.
        mainMenu = new MenuBar();  //Creates our main menu to hold our Sub-Menus.
        toolBar = new ToolBar();  //Creates our tool-bar to hold the buttons.

        tabPane = new TabPane(); // Use tab pane with one tab for sizing UI and one tab for alignment UI

        createToolBar();

        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(25, 25, 25, 25));

        topContainer.getChildren().add(mainMenu);
        borderPane.setTop(topContainer);

        rootPane.setStyle(
                "-fx-background-image: url('" +
                        Main.class.getResource("/app/resources/dragndrop.png").toExternalForm() +
                        "'); " +
                        "-fx-background-position: center center; " +
                        "-fx-background-repeat: stretch;"
        );

        selectFileButton.setAlignment(Pos.CENTER);
        gridPane.getChildren().addAll(selectFileButton);

        rootPane.getChildren().addAll(gridPane, borderPane);
        borderPane.setCenter(gridPane);

        alertBox = new Alert(Alert.AlertType.ERROR);
        progressWindow = new ProgressForm();

        scene = new Scene(rootPane, 960, 720);
        primaryStage.setTitle("TS Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();

        detailTab = new DetailTab(nodes);
        visualizationTab = new VisualizationTab(scene);
        graphTab = new GraphTab(scene);
    }


    private void createToolBar() {

        fileChooser = new FileChooser();
        selectFileButton = new Button("Select file...");

        file = new Menu("File");
        edit = new Menu("Edit");
        help = new Menu("Help");

        openFile = new MenuItem("Open File");
        importXML = new MenuItem("Import XML");
        exportXML = new MenuItem("Export XML");
        settings = new MenuItem("Settings");
        exitApp = new MenuItem("Exit");

        jumpToPacket = new MenuItem("Jump to packet...");
        searchPacket = new MenuItem("Search packet...");

        about = new MenuItem("About");
        manual = new MenuItem("Manual");

        importXML.setDisable(true);
        exportXML.setDisable(true);
        settings.setDisable(true);
        jumpToPacket.setDisable(true);
        searchPacket.setDisable(true);
        manual.setDisable(true);

        file.getItems().addAll(openFile, importXML, exportXML, new SeparatorMenuItem(), settings, new SeparatorMenuItem(), exitApp);
        edit.getItems().addAll(jumpToPacket, searchPacket);
        help.getItems().addAll(manual, new SeparatorMenuItem(), about);

        mainMenu.getMenus().addAll(file, edit, help);
    }


    public void createTask(Stream streamDescriptor) {

        this.task = new Task() {
            @Override
            public Object call() throws InterruptedException, IOException {
                Platform.runLater(() -> {
                    rootPane.setStyle("-fx-background-color: transparent");
                    detailTab.setNodes(streamDescriptor);
                    visualizationTab.visualizePackets(streamDescriptor);
                    graphTab.drawGraph(streamDescriptor);
                    createTabPane();
                });
                return null;
            }
        };
    }


    public void showAlertBox(String title, String content) {
        alertBox.setTitle(title);
        alertBox.setContentText(content);
        alertBox.setHeaderText(null);
        alertBox.show();
    }


    public void createTabPane() {
        detailTab.tab.setContent(detailTab.createTreeTab());
        tabPane.getTabs().addAll(detailTab.tab, visualizationTab.tab, graphTab.tab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        borderPane.setCenter(tabPane);
        rootPane.getChildren().clear();
        rootPane.getChildren().addAll(borderPane);
    }


    public void showAbout() {
        aboutStage.initModality(Modality.WINDOW_MODAL);
        aboutStage.setTitle("About");

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(5));
        vBox.getChildren().addAll(new Label("TS Visualizer BETA" + "\n\n" + "Last release " + releaseDate), new Text("\nCreated by Tomas Krupa"));

        Scene scene = new Scene(vBox, 400, 300);
        aboutStage.setScene(scene);
        aboutStage.show();
    }

    public Task getTask() {
        return task;
    }
}