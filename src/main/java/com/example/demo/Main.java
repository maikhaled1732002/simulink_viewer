package com.example.demo;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main extends Application {
    private static List<Block> blocks = new ArrayList<>();
    private static List<Arrow> connections = new ArrayList<>();
    private static Group root = new Group();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException, ParserConfigurationException, SAXException {
        Scene scene = new Scene(root, 1500, 790);
        stage.setTitle("Simulink viewer");

        Image image = new Image("1.png");
        ImageView imageView = new ImageView(image);
        stage.getIcons().add(imageView.getImage());

        mdlParsing();
        drawBlocks();
        drawArrows();

        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
        mouseEvents(scene, stage);
        stage.show();
    }

    public static void mdlParsing() throws IOException, ParserConfigurationException, SAXException {
        File file = new File("untitled.mdl");
        FileInputStream input = new FileInputStream(file);
        StringBuilder mdlFile = new StringBuilder();
        int q;
        while ((q = input.read()) != -1) {
            mdlFile.append((char) q);
        }
        String mdlFileS = mdlFile.toString();
        Scanner scanner = new Scanner(mdlFileS);
        StringBuilder a = new StringBuilder();
        String before = "-1";
        boolean now = false;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals("<System>")) {
                now = true;
            } else if (line.equals("</System>")) {
                a.append(before).append('\n');
                a.append(line);
                break;
            }
            if (now) {
                a.append(before).append('\n');
            }
            before = line;
        }
        String newMdlFile = a.toString();
        String outputFileName = "neededFile.mdl";
        FileOutputStream outputStream = new FileOutputStream(outputFileName);
        outputStream.write(newMdlFile.getBytes());

        file = new File("neededFile.mdl");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        Element rootElement = doc.getDocumentElement();
        doc.getDocumentElement().normalize();

        addBlocks(rootElement, doc);
        addArrows(rootElement, doc);
    }

    public static void addBlocks(Element rootElement, Document doc) {
        if (rootElement.getTagName().equals("System")) {
            NodeList blockList = doc.getElementsByTagName("Block");
            for (int i = 0; i < blockList.getLength(); i++) {
                boolean inputs_ports_position_flag = false;
                boolean blockMirror = false;
                Node blockNode = blockList.item(i);
                NodeList childNodes = blockNode.getChildNodes();
                if (blockNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element blockElement = (Element) blockNode;

                    String name = blockElement.getAttribute("Name");
                    String name = blockElement.getAttribute("Name");
                    String type = blockElement.getAttribute("Type");
                    String position = blockElement.getAttribute("Position");

                    // Parse position attribute to get x and y coordinates
                    String[] positionCoords = position.split(" ");
                    double x = Double.parseDouble(positionCoords[0]);
                    double y = Double.parseDouble(positionCoords[1]);

                    // Create a new Block object and add it to the blocks list
                    Block block = new Block(name, type, x, y);
                    blocks.add(block);

                    // Iterate through child nodes to find input ports and their positions
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node childNode = childNodes.item(j);
                        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element childElement = (Element) childNode;
                            if (childElement.getTagName().equals("Ports")) {
                                NodeList portList = childElement.getElementsByTagName("Port");
                                for (int k = 0; k < portList.getLength(); k++) {
                                    Node portNode = portList.item(k);
                                    if (portNode.getNodeType() == Node.ELEMENT_NODE) {
                                        Element portElement = (Element) portNode;
                                        String portType = portElement.getAttribute("Type");
                                        String portPosition = portElement.getAttribute("Position");

                                        // Parse port position attribute to get x and y coordinates
                                        String[] portPositionCoords = portPosition.split(" ");
                                        double portX = Double.parseDouble(portPositionCoords[0]);
                                        double portY = Double.parseDouble(portPositionCoords[1]);

                                        // Create a new Port object and add it to the current block
                                        Port port = new Port(portType, portX, portY);
                                        block.addPort(port);

                                        // Set the flag to true if input ports are found
                                        if (portType.equals("Inport")) {
                                            inputs_ports_position_flag = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Set the block mirror flag based on the presence of input ports
                    block.setBlockMirror(inputs_ports_position_flag);
                }
            }
        }
    }

    public static void addArrows(Element rootElement, Document doc) {
        if (rootElement.getTagName().equals("System")) {
            NodeList lineList = doc.getElementsByTagName("Line");
            for (int i = 0; i < lineList.getLength(); i++) {
                Node lineNode = lineList.item(i);
                if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element lineElement = (Element) lineNode;

                    String source = lineElement.getAttribute("SrcBlock");
                    String sourcePort = lineElement.getAttribute("SrcPort");
                    String destination = lineElement.getAttribute("DstBlock");
                    String destinationPort = lineElement.getAttribute("DstPort");

                    // Find the source and destination blocks based on their names
                    Block sourceBlock = findBlockByName(source);
                    Block destinationBlock = findBlockByName(destination);

                    // Create a new Arrow object and add it to the connections list
                    Arrow arrow = new Arrow(sourceBlock, sourcePort, destinationBlock, destinationPort);
                    connections.add(arrow);
                }
            }
        }
    }

    public static Block findBlockByName(String name) {
        for (Block block : blocks) {
            if (block.getName().equals(name)) {
                return block;
            }
        }
        return null;
    }

    public static void drawBlocks() {
        for (Block block : blocks) {
            double x = block.getX();
            double y = block.getY();
            String type = block.getType();

            // Draw the block based on its type and position
            if (type.equals("SubSystem")) {
                drawSubSystemBlock(x, y);
            } else if (type.equals("Function")) {
                drawFunctionBlock(x, y);
            } else if (type.equals("Signal")) {
                drawSignalBlock(x, y);
            }

            // Draw input and output ports
            ArrayList<Port> ports = block.getPorts();
            for (Port port : ports) {
                double portX = port.getX();
                double portY = port.getY();
                String portType = port.getType();

                if (portType.equals("Inport")) {
                    drawInputPort(portX, portY);
                } else if (portType.equals("Outport")) {
                    drawOutputPort(portX, portY);
                }
            }
        }
    }

    public static void drawConnections() {
        for (Arrow arrow : connections) {
            Block sourceBlock = arrow.getSourceBlock();
            Block destinationBlock = arrow.getDestinationBlock();

            double sourceX = sourceBlock.getX();
            double sourceY = sourceBlock.getY();
            double destinationX = destinationBlock.getX();
            double destinationY = destinationBlock.getY();

            String sourcePort = arrow.getSourcePort();
            String destinationPort = arrow.getDestinationPort();

            // Draw the connection between the source and destination ports
            if (sourcePort.equals("Outport")) {
                drawArrow(sourceX, sourceY, destinationX, destinationY);
            } else if (destinationPort.equals("Inport")) {
                drawArrow(destinationX, destinationY, sourceX, sourceY);
            }
        }
    }

    public static void main(String[] args) {
        // Load XML file
        File xmlFile = new File("system.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Get the root element
            Element rootElement = doc.getDocumentElement();

            // Call the methods to extract and visualize the system
            extractBlocks(rootElement, doc);
            addArrows(rootElement, doc);
            drawBlocks();
            drawConnections();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



public class Port {
    private String type;
    private double x;
    private double y;

    public Port(String type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
