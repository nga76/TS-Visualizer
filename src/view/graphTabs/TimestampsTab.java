package view.graphTabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

import model.Stream;
import model.Tables;
import model.config.MPEG;
import view.visualizationTab.VisualizationTab;

import static model.Sorter.sortHashMapByKey;
import static model.config.MPEG.TimestampType.DTS;
import static model.config.MPEG.TimestampType.PTS;
import static model.config.MPEG.nil;


public class TimestampsTab extends VisualizationTab implements Graph{

    private Scene scene;
    public Tab tab;
    private Stream stream;

    private Label captionLabel;
    private ScatterChart scatterChart;
    private ComboBox<String> filterComboBox;

    private EventHandler<ActionEvent> filterComboBoxEvent;

    private Map filteredPIDs;

    public static final int tickUnit = 10;
    private HBox filterHBox;

    private Map tooltips;
    private VBox vbox;


    public TimestampsTab(){
        tab = new Tab("Timestamps");
        captionLabel = new Label("");
        tooltips = new HashMap();
    }


    public void drawGraph(Stream streamDescriptor) {

        this.stream = streamDescriptor;

        filterComboBox = createFilterComboBox(stream);
        filterComboBox.setValue(filterComboBox.getItems().get(2));

        filterHBox = new HBox(new Label("Program filter: "), filterComboBox);
        filterHBox.setAlignment(Pos.CENTER);
        filterHBox.setSpacing(10);
        filterHBox.setPadding(new Insets(10,10,10,10));

        captionLabel.setTextFill(Color.DARKORANGE);
        captionLabel.setStyle("-fx-font: 24 arial;");
        captionLabel.toFront();

        scatterChart = createScatterChart(stream.getTables(),filterComboBox.getValue());
        addListenersAndHandlers(scatterChart);
        filterComboBox.setOnAction(filterComboBoxEvent);

        vbox = new VBox(captionLabel,scatterChart,filterHBox);
        tab.setContent(vbox);
    }


    private<K,V>ScatterChart createScatterChart(Tables tables, String selectedService) {
        tooltips.clear();
        final ScatterChart scatterChart = createScaledScatterChart(tables, selectedService);

        for (Map.Entry<Integer, String> program : ((Map<Integer, String>) tables.getProgramMap()).entrySet()) {

            if (selectedService.equals("All") || program.getValue().equals(selectedService)) {
                for (Map.Entry<Integer, Map<MPEG.TimestampType, Map<Long, Long>>> serviceTimestamps : ((Map<Integer, Map<MPEG.TimestampType, Map<Long, Long>>>) tables.getServiceTimestampMap()).entrySet()) {

                    if (program.getKey().equals(serviceTimestamps.getKey())) {
                        for (Map.Entry<MPEG.TimestampType, Map<Long, Long>> timestampMap : serviceTimestamps.getValue().entrySet()) {

                            XYChart.Series series = new XYChart.Series();
                            series.setName(timestampMap.getKey().toString());
                            series.getData().add(new XYChart.Data(nil, nil)); //default entry for legend displaying

                            for (Map.Entry<Long, Long> packetEntry : timestampMap.getValue().entrySet()) {
                                XYChart.Data data = new XYChart.Data(packetEntry.getValue(), packetEntry.getKey());
                                series.getData().add(data);
                                String type = "PCR";
                                int PID = 2102;
                                int position = 1832;
                                String time = parseTimestamp(8137976234L);
//                                Tooltip tooltip =  new Tooltip("Type: " + type + "\nPID: " + PID + "\nTime: " + time + "\nPacket position: " + position);
//                                Tooltip.install(data.getNode(), tooltip);
                                tooltips.put( data.hashCode() , new Tooltip("Type: " + type + "\nPID: " + PID + "\nTime: " + time + "\nPacket position: " + position) );
                            }
                            scatterChart.getData().add(series);
                        }
                    }
                }
            }
        }
        scatterChart.setTitle("Timestamps layout");
        scatterChart.setLegendSide(Side.LEFT);
        scatterChart.toBack();
        scatterChart.setPadding(new Insets(10,40,10,40));
        scatterChart.setPrefHeight(scene.getHeight());

        return scatterChart;
    }

    private ScatterChart createScaledScatterChart(Tables tables, String selectedService) {
        Long maxX = 0L;
        Long maxY = 0L;
        for (Map.Entry<Integer, String> program : ((Map<Integer, String>) tables.getProgramMap()).entrySet()) {
            if (selectedService.equals("All") || program.getValue().equals(selectedService)) {
                for (Map.Entry<Integer, Map<MPEG.TimestampType, Map<Long, Long>>> serviceTimestamps : ((Map<Integer, Map<MPEG.TimestampType, Map<Long, Long>>>) tables.getServiceTimestampMap()).entrySet()) {
                    if (program.getKey().equals(serviceTimestamps.getKey())) {
                        for (Map.Entry<MPEG.TimestampType, Map<Long, Long>> timestampMap : serviceTimestamps.getValue().entrySet()) {
                            for (Map.Entry<Long, Long> packetEntry : timestampMap.getValue().entrySet()) {
                                if (packetEntry.getValue().longValue() > maxX.longValue()) {
                                    maxX = packetEntry.getValue();
                                }
                                if (packetEntry.getKey().longValue() > maxY.longValue()) {
                                    maxY = packetEntry.getKey();
                                }
                            }
                        }
                    }
                }
            }
        }
        final NumberAxis xAxis = new NumberAxis(0, Double.valueOf(maxX*1.05f).longValue(), Double.valueOf(maxX*0.08f).longValue());
        final NumberAxis yAxis = new NumberAxis(0, Double.valueOf(maxY*1.05f).longValue(), Double.valueOf(maxY*0.08f).longValue());
        xAxis.setLabel("Packet order");
        yAxis.setLabel("Time");

        return new ScatterChart<>(xAxis, yAxis);
    }


    public void addListenersAndHandlers(Chart chart) {
        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            chart.setPrefHeight(scene.getHeight());
        });

        filterComboBoxEvent = (ActionEvent event) -> {
            scatterChart = createScatterChart(stream.getTables(), filterComboBox.getValue());
            vbox = new VBox(captionLabel, scatterChart, filterHBox);
            tab.setContent(vbox);
        };

        for (XYChart.Series<Number, Number> series : ((XYChart<Number, Number>) chart).getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                data.getNode().setOnMousePressed(event -> {
                    Tooltip tooltip = ((Tooltip) tooltips.get(data.hashCode()));
                    if (!tooltip.isActivated()) {
                        tooltip.show(vbox, event.getScreenX(), event.getScreenY());
                        for (Object currentTooltip : tooltips.values()) {
                            if (!currentTooltip.equals(tooltip)) {
                                ((Tooltip) currentTooltip).hide();
                            }
                        }
                    }
                });
                data.getNode().setOnMouseReleased(event -> {
                    if (((Tooltip) tooltips.get(data.hashCode())).isActivated()) {
                        ((Tooltip) tooltips.get(data.hashCode())).hide();
                    }
                });
            }
        }
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }
}
