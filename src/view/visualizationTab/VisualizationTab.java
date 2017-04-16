package view.visualizationTab;

import model.Stream;
import model.TSpacket;
import model.Sorter;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import view.Window;

import java.util.*;

import static model.config.Config.*;
import static model.config.DVB.isPSI;
import static model.config.DVB.nil;


public class VisualizationTab extends Window{

    public Tab tab;
    Stream stream;
    private Sorter sorter;
    private Scene scene;
    private CheckBox groupByCheckBox;
    private ComboBox<String> filterComboBox;
    Map sortedPIDs;
    HashMap originalPIDmap;
    Map filteredPIDs;
    public ArrayList<TSpacket> packets;
    private Slider zoomer;
    private PacketPane packetPane;
    private BarPane barPane;
    LegendPane legendPane;
    private EventHandler<ActionEvent> groupByCheckBoxEvent, programComboBoxEvent, zoomerEvent;

    public VisualizationTab() {
        super();
        tab = new Tab("Visualization");
        sorter = new Sorter();
    }


    public void init(Scene scene){
        this.scene = scene;
        packetPane = new PacketPane(this.scene);
        barPane = new BarPane(this.scene);
        legendPane = new LegendPane(this.scene);
    }


    public void visualizePackets(Stream stream) {

        this.stream = stream;

        packetPane.setLegendPane(legendPane);
        packetPane.setBarPane(barPane);
        legendPane.setPacketPane(packetPane);
        legendPane.setBarPane(barPane);
        barPane.setPacketPane(packetPane);
        barPane.setLegendPane(legendPane);

        packets = stream.getPackets();
        originalPIDmap = new HashMap<>(stream.getMapPIDs());
        sortedPIDs = ungroup(originalPIDmap);

        packetPane.createScrollPane(stream, packets, sortedPIDs, stream.getMapPIDs().size());
        barPane.createScrollPane(stream, packets, sortedPIDs, stream.getMapPIDs().size());
        legendPane.createScrollPane(stream, packets, sortedPIDs, stream.getMapPIDs().size());

        packetPane.drawCanvas(stream, packets,0);
        legendPane.drawCanvas(stream, packets,0);

        Map<Integer, Integer> sortedMapPIDs = new LinkedHashMap<>(sorter.sortHashMapByKey(sortedPIDs));
        Map labeledPIDs = createLabeledPIDs(sortedMapPIDs, stream.getTables().getPMTmap(), null);
        legendPane.createLabels(labeledPIDs);

        HBox labelsLegendScrollPaneBox = new HBox(legendPane.labelScrollPane, legendPane.scrollPane);

        HBox comboCheckboxBar = createComboCheckBoxBar(stream);

        VBox mainVBox = new VBox(comboCheckboxBar, packetPane.scrollPane, barPane.scrollPane, labelsLegendScrollPaneBox);

        VBox.setMargin(packetPane.scrollPane, new Insets(inset, inset, 0, inset));
        VBox.setMargin(barPane.scrollPane, new Insets(0, inset, 0, inset));
        VBox.setMargin(labelsLegendScrollPaneBox, new Insets(0, inset, inset, inset));

        addListenersAndHandlers();

        tab.setContent(mainVBox);
    }


    private HBox createComboCheckBoxBar(Stream stream) {

        filterComboBox = createFilterComboBox(stream);
        filterComboBox.setOnAction(programComboBoxEvent);

        groupByCheckBox = new CheckBox("Group by programmes");
        groupByCheckBox.setOnAction(groupByCheckBoxEvent);

        Label filterLabel = new Label("Filter:");
        Label zoomerLabel = new Label("Zoom:");

        zoomer = new Slider(0,100,50);

        HBox comboCheckboxBar = new HBox(filterLabel, filterComboBox, groupByCheckBox, zoomerLabel, zoomer);
        HBox.setMargin(filterComboBox, new Insets(inset, inset, inset, inset));
        HBox.setMargin(groupByCheckBox, new Insets(2*inset, 7*inset, inset, 7*inset));
        HBox.setMargin(filterLabel, new Insets(2*inset, inset, inset, inset));
        HBox.setMargin(zoomerLabel, new Insets(2*inset, inset, inset, 10*inset));
        HBox.setMargin(zoomer, new Insets(2*inset, inset, inset, inset));

        return comboCheckboxBar;
    }


    private ComboBox<String> createFilterComboBox(Stream stream) {

        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.getItems().add("All");
        comboBox.getSelectionModel().selectFirst();

        for (Object entry : stream.getPrograms().values()) {
            comboBox.getItems().add(entry.toString());
        }
        return comboBox;
    }


    public boolean isInViewport(Scene scene, double packetPosition, double start) {
        double end = start + scene.getWidth();
        return packetPosition >= start && packetPosition <= end;
    }


    private void addListenersAndHandlers() {

        groupByCheckBoxEvent = event -> {
            groupProgrammes(filteredPIDs,stream.getTables().getPMTmap());
        };

        programComboBoxEvent = (ActionEvent event) -> {
            filteredPIDs = filterProgram(stream.getTables().getPMTmap());
            groupProgrammes(filteredPIDs,stream.getTables().getPMTmap());
        };

        zoomer.valueProperty().addListener((ov, old_val, new_val) -> {
            //TODO implement zoomer
            packetPane.scrollPane.setScaleX( 1 + ((new_val.doubleValue()-50) / 50));
            packetPane.scrollPane.setScaleY( 1 + ((new_val.doubleValue()-50) / 50));
        });

        groupByCheckBox.setOnAction(groupByCheckBoxEvent);
        filterComboBox.setOnAction(programComboBoxEvent);
    }


    private <K, V> Map filterProgram(Map<K, V> PMTmap) {

        String selectedProgram = filterComboBox.getValue();
        if (selectedProgram.equals("All")){
            return null;
        }
        Integer programPID = (Integer)getByValue(stream.getPrograms(), selectedProgram);

        Map filteredMap = new HashMap();
        for (Map.Entry<K, V> entry : PMTmap.entrySet()) {
            if(entry.getValue().equals(programPID)) {
                filteredMap.put(entry.getKey(), entry.getValue());
                System.out.println(entry.getKey() + ":" + entry.getValue());
            }
        }
        return filteredMap;
    }


    public <K, V> K getByValue(Map<K,V> map, V value) {
        return map.entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }


    private void groupProgrammes(Map filteredPIDs, Map PMTmap) {

        if (groupByCheckBox.isSelected()) {
            sortedPIDs = groupByProgrammes(filteredPIDs, originalPIDmap, PMTmap);
            Map labeledPIDs = createLabeledPIDs(null, PMTmap, sortedPIDs);
            updatePanes(sortedPIDs,labeledPIDs);
        }
        else {
            Map PIDs = (filteredPIDs != null) ? filteredPIDs : originalPIDmap;
            sortedPIDs = ungroup(PIDs);
            Map<Integer, Integer> sortedMapPIDs = new LinkedHashMap<>(sorter.sortHashMapByKey(sortedPIDs));
            Map labeledPIDs = createLabeledPIDs(sortedMapPIDs,PMTmap,null);
            updatePanes(sortedPIDs,labeledPIDs);
        }
    }


    private void updatePanes(Map sortedPIDs, Map labeledPIDs) {
        //TODO legend pane jump bug
        packetPane.setSortedPIDs(sortedPIDs);
        legendPane.setSortedPIDs(sortedPIDs);
        barPane.setSortedPIDs(sortedPIDs);
        packetPane.drawPackets(stream,packets,packetPane.getXpos());
        legendPane.drawPackets(stream,packets,packetPane.getXpos());
        legendPane.createLabels(labeledPIDs);
    }


    private Map createLabeledPIDs(Map<Integer, Integer> sortedMapPIDs, Map PMTmap, Map<Integer, Integer> gruppedPMTmap) {
        Map resultMap = new HashMap();
        if(gruppedPMTmap==null){
            for (Map.Entry<Integer, Integer> entry : sortedMapPIDs.entrySet()) {
                resultMap.put(entry.getKey(),"PID: ");
            }
        }
        else{
            int previous = nil;
            for (Map.Entry<Integer, Integer> entry : gruppedPMTmap.entrySet()) {
                if ( previous != entry.getValue() ){
                    Integer PID = entry.getKey();
                    previous = entry.getValue();

                    if(!isPSI(PID) && stream.getPrograms().get(PMTmap.get(PID)) != null){
                        resultMap.put(PMTmap.get(PID),"Program: ");
                    }
                    else {
                        resultMap.put(PID, "PID: ");
                    }
                }
            }
        }
        Map map = sorter.sortHashMapByKey(resultMap);
        return map;
    }


    private <K,V> Map groupByProgrammes(Map<K,V> filteredPIDs, Map originalPIDmap, Map PMTmap) {
        HashMap gruppedMap = new HashMap<Integer,Integer>();

        if( filteredPIDs == null ){
            Map<Integer,Integer> enhancedPMTmap = enhancePMTmap(originalPIDmap, PMTmap);
            int index = nil;
            Integer previous = nil;
            for (Map.Entry<Integer,Integer> entry : enhancedPMTmap.entrySet()) {
                if (!previous.equals(entry.getValue())) {
                    previous = entry.getValue();
                    index++;
                }
                gruppedMap.put(entry.getKey(), index);
            }
        }
        else {
            for (Map.Entry<K,V> entry : filteredPIDs.entrySet()) {
                gruppedMap.put(entry.getKey(), 0);
            }
        }
        return sorter.sortHashMapByValue(gruppedMap);
    }


    private <K,V> Map<Integer,Integer> enhancePMTmap(Map<K,V> originalPIDmaps, Map<K,V> PMT ) {

        for (Map.Entry<K,V> entry : (originalPIDmaps).entrySet()) {
            PMT.putIfAbsent(entry.getKey(),(V)entry.getKey());
        }
        return sorter.sortHashMapByValue((Map<Integer,Integer>)PMT);
    }


    private HashMap ungroup(Map originalPIDmaps) {
        List<Integer> sorted = sorter.sortMapToListByKey(originalPIDmaps);
        HashMap ungruppedMap = new HashMap<Integer,Integer>();

        for(Integer item : sorted){
            ungruppedMap.put(item,sorted.indexOf(item));
        }
        return ungruppedMap;
    }


    boolean isPMT(Map PATmap, int PID) {
        return getByValue(PATmap,PID) != null;
    }
}

