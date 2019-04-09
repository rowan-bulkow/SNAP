//
// Copyright Martin Cenek <drcenek@gmail.com> 2016-2019
//
// All source code is released under the terms of the MIT License.
// See LICENSE for more information.
// Contributions from: 
// Eric Pak, Levi Oyster, Boyd Ching, Rowan Bulkow, Neal Logan, Mackenzie Bartlett
//

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.util.Arrays.sort;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CountDownLatch;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.graph.api.Column;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.appearance.api.Function;
// import org.gephi.datalab.api.AttributeColumn;
// import org.gephi.datalab.api.AttributeController;
// import org.gephi.datalab.api.AttributeModel;
// import org.gephi.dynamic.api.DynamicController;
import org.gephi.filters.api.FilterController;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.UndirectedGraph;
//import org.gephi.graph.api.DirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
// import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
// import org.gephi.io.processor.plugin.DynamicProcessor;
import org.gephi.layout.plugin.circularlayout.circlelayout.CircleLayout;
import org.gephi.layout.plugin.circularlayout.radialaxislayout.RadialAxisLayout;
// import org.gephi.partition.api.Partition;
// import org.gephi.partition.api.PartitionController;
// import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
// import org.gephi.ranking.api.Ranking;
// import org.gephi.ranking.api.RankingController;
// import org.gephi.ranking.api.Transformer;
// import org.gephi.ranking.plugin.transformer.AbstractColorTransformer;
// import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.Modularity;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.filters.plugin.graph.EgoBuilder.EgoFilter;
import org.gephi.filters.plugin.operator.INTERSECTIONBuilder.IntersectionOperator;
import org.gephi.filters.plugin.partition.PartitionBuilder.NodePartitionFilter;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;

import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 *
 * @author motion411
 */
public class AutoGephiPipe
{
    private static ProjectController pc;
    private static Workspace workspace;
    private static GraphModel graphModel;
    // private static AttributeModel attributeModel;
    private static ImportController importController;
    private static AppearanceController appearanceController;
    private static AppearanceModel appearanceModel;

    private static Graph graph;
    // private static String processedFile;
    // private static DynamicProcessor dynamicProcessor;
    // private static Partition partition;
    // private static Container container;
    public static Scanner read;
    public static String year, month,day;
    public static String sizeNodesBy;
    public static String dates[] = new String[3000];
    public static int dateCounter = 0;
    public static double modResolution = 0.4;

    private static String badfileDateRegex = "^[a-zA-Z0-9/*-]+((([0-1][0-9]{3})|([2][0][0-9]{2}))[-]"
        + "(([0][1-9])|([1][0-2]))[-](([0][1-9])|([1-2][0-9])|([3][0-1]))+).*";
    private static String fileDateRegex = "[0-9]{4}[-][0-9]{2}[-][0-9]{2}";
    private static Pattern fileDatePattern = Pattern.compile(fileDateRegex);

    private static String interiorDateRegex = "-?\\d+";
    private static Pattern interiorDatePattern = Pattern.compile(interiorDateRegex);

    public static final String CIRCULAR_STAR_LAYOUT = "0";
    public static final String RADIAL_AXIS_LAYOUT = "1";
    public static final String YIFAN_HU_LAYOUT = "2";
    public static final String FORCE_ATLAS_LAYOUT = "3";

    public static final String BETWEENNESS = "Betweenness";
    public static final String CLOSENESS = "Closeness";
    public static final String DEGREE = "Degree";

    // Initialize a project and a workspace
    public static void initialize()
    {
        // Project must be created to use toolkit features
        // Creating a new project creates a new workspace, Workspaces are containers of all data
        pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        importController = Lookup.getDefault().lookup(ImportController.class);
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        graph = graphModel.getGraph();
        appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        appearanceModel = appearanceController.getModel();
        // attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();

        graphModel.getNodeTable().addColumn("date", "".getClass());

        // Initialize the DynamicProcessor - which will append the container to the workspace
        // dynamicProcessor = new DynamicProcessor();
        // dynamicProcessor.setDateMode(true); // Set 'true' if you set real dates (ex: yyyy-mm-dd), it's double otherwise
        // dynamicProcessor.setLabelmatching(true); // Set 'true' if node matching is done on labels instead of ids

        // graph = graphModel.getGraph();
        // graph.readUnlockAll();
    }

    // Runs Radial Axis layout, each Spire represents a separate community
    public static void radialAxLayout()
    {
        RadialAxisLayout radLayout = new RadialAxisLayout(null,1.0,false);
        radLayout.setGraphModel(graphModel);
        radLayout.resetPropertiesValues();
        radLayout.setScalingWidth(1.0);

        // Node placement
        // Makes each Spar a seperate Modularity Class
        radLayout.setNodePlacement(Modularity.MODULARITY_CLASS+"-Att");

        // Nodes are positioned in spar by these selected measures
        if(sizeNodesBy=="Betweenness")
        {
            radLayout.setSparNodePlacement(GraphDistance.BETWEENNESS+"-Att");
        }
        else if(sizeNodesBy=="Closeness")
        {
            radLayout.setSparNodePlacement(GraphDistance.CLOSENESS+"-Att");
        }
        else if(sizeNodesBy=="Degree")
        {
            try
            {
                // radLayout.setSparNodePlacement(Ranking.DEGREE_RANKING+"-Att"); // Currently does nothing.
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return;
            }
        }
        radLayout.setSparSpiral(true);
        radLayout.setSparOrderingDirection(Boolean.FALSE);
        radLayout.setKnockdownSpars(Boolean.FALSE);

        radLayout.initAlgo(); // start algorithm
        for(int i=0; i<100 && radLayout.canAlgo(); i++)
        {
            radLayout.goAlgo();
        }
    }
    public static void circLayout()
    {
        CircleLayout circLayout=new CircleLayout(null,1.0,true);
        circLayout.setGraphModel(graphModel);
        circLayout.resetPropertiesValues();
        circLayout.setNodePlacementNoOverlap(Boolean.TRUE);
        //circLayout.setNodePlacement(Modularity.MODULARITY_CLASS+"-Att");
        circLayout.setNodePlacement(GraphDistance.BETWEENNESS+"-Att");
        circLayout.initAlgo();

        for(int i=0; i<1000 && circLayout.canAlgo(); i++)
        {
            circLayout.goAlgo();
        }
    }
    public static void yifanHuLayout()
    {
        YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
        layout.setGraphModel(graphModel);
        layout.initAlgo();
        layout.resetPropertiesValues();
        layout.setOptimalDistance(200f);

        for (int i = 0; i < 1000 && layout.canAlgo(); i++) {
           layout.goAlgo();
        }
        layout.endAlgo();
    }
    public static void forceAtlasLayout()
    {
        ForceAtlasLayout layout = new ForceAtlasLayout(null);
        layout.setGraphModel(graphModel);
        layout.initAlgo();
        layout.resetPropertiesValues();

        for (int i = 0; i < 1000 && layout.canAlgo(); i++) {
           layout.goAlgo();
        }
        layout.endAlgo();
    }

    // Resize nodes by centrality measures or degree, further categories can be added later
    public static void sizeNodes()
    {
        // Get Centrality and then size nodes by measure
        GraphDistance distance = new GraphDistance();
        distance.setDirected(false);
        distance.execute(graphModel);

        // Size by Betweeness centrality
        // RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
        // Ranking degreeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, Ranking.DEGREE_RANKING);
        // Default to Size by Betweenness
        // AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);

        // Set Size by different centralities base on input
        // if(sizeNodesBy == "Betweenness")
        // {
        //     // centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        // }
        // else if(sizeNodesBy == "Closeness")
        // {
        //     // centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.CLOSENESS);
        // }
        // TODO: find proper input to utilize Eigenvector
        // else if(sizeNodesBy=="Eigenvector")
        // {
        //     centralityColumn = attributeModel.getNodeTable().getColumn(EIGENVECTOR);
        // }

        // Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());
        // AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel()
        //     .getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);

        // sizeTransformer.setMinSize(20);
        // sizeTransformer.setMaxSize(100);
        // rankingController.transform(centralityRanking, sizeTransformer);
        // Seperate case since not compatible with centrality column
        // if(sizeNodesBy == "Degree")
        // {
        //     // rankingController.transform(degreeRanking, sizeTransformer);
        // }

        if(sizeNodesBy == AutoGephiPipe.CLOSENESS)
        {
            Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.CLOSENESS);
            Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
            RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
            centralityTransformer.setMinSize(3);
            centralityTransformer.setMaxSize(10);
            appearanceController.transform(centralityRanking);
        }
        else if(sizeNodesBy == AutoGephiPipe.DEGREE)
        {
            Function centralityRanking = appearanceModel.getNodeFunction(
                graph,
                AppearanceModel.GraphFunction.NODE_DEGREE,
                RankingNodeSizeTransformer.class);
            RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
            centralityTransformer.setMinSize(3);
            centralityTransformer.setMaxSize(10);
            appearanceController.transform(centralityRanking);
        }
        else if(sizeNodesBy == AutoGephiPipe.BETWEENNESS)
        {
            Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
            Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
            RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
            centralityTransformer.setMinSize(3);
            centralityTransformer.setMaxSize(10);
            appearanceController.transform(centralityRanking);
        }
        else
        {
            System.err.println("No sizing meteric specified.");
        }
    }

    public static void colorByCommunity()
    {
        // Color by Community but running modularity measures
        // PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        System.out.println("Passed Partion Controller");

        // Run modularity algorithm - community detection
        Modularity modularity = new Modularity();
        if(modularity!=null)
        {
            System.out.println("Passed New Modularity");
        }

        modularity.setResolution(modResolution);
        System.out.println("Passed Modularity Resolution");
        try
        {
            // modularity.execute(graphModel, attributeModel);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("Failed Modularity Execute");
        }

        System.out.println("Passed Modularity Execute");

        // Partition with 'modularity_class', just created by Modularity algorithm
        // AttributeColumn modColumn = attributeModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        System.out.println("Passed Modularity Column");
        // AttributeColumn timeColumn=attributeModel.getNodeTable().
        // partition = partitionController.buildPartition(modColumn, graph);
        System.out.println("Passed Modularity Partition");
        // System.out.println(partition.getPartsCount() + " Communities found");
        // NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
        // nodeColorTransformer2.randomizeColors(partition);
        // partitionController.transform(partition, nodeColorTransformer2);
    }
    // UNused should be deleted!
    public static void runModularity()
    {
        // Color by Community but running modularity measures
        // PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        // Run modularity algorithm - community detection
        Modularity modularity = new Modularity();
        modularity.setResolution(modResolution);
        // modularity.execute(graphModel, attributeModel);
    }

    public static void circularStarLayout()
    {
        int communityCount = 42;
        try
        {
            // communityCount=partition.getPartsCount();
        }
        catch(Exception e){
            e.printStackTrace();
            return;
        }
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);

        // NodePartitionFilter partitionFilter = new NodePartitionFilter(partition);
        Query query2; // Queries are ran on filtered partitions
        GraphView view2;

        double rankingByPercent[] = new double[communityCount];

        int placementQuadrant[]=new int[communityCount];
        // As community's are placed in quadrants, increment these shifts as multipliers of placement
        float shifts[]={1,1,1,1,1,1,1,1,1,1,1,1};
        for(int i=0; i<=communityCount-1;i++)
        {
            // partitionFilter.unselectAll();
            // partitionFilter.addPart(partition.getPartFromValue(i)); // Makes a community Active

            try
            {
                graph.readUnlockAll();
                graphModel = graph.getModel();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return;
            }
            // query2 = filterController.createQuery(partitionFilter);
            // view2 = filterController.filter(query2);
            // graphModel.setVisibleView(view2);
            // Assigns Partition Node Percentage to Array for sorting
            for(Node n : graphModel.getGraphVisible().getNodes())
            {
                // rankingByPercent[i]=partitionFilter.getPartition().getPart(n).getPercentage();
            }
        }
        //sort(rankingByPercent);
        double sortedPercent []=new double[rankingByPercent.length];
        for(int i=0; i<sortedPercent.length;i++)
        {
            sortedPercent[i]=rankingByPercent[i];
        }

        sort(sortedPercent); // Orders from smallest to largest percentage of nodes
        for(int i =0; i<rankingByPercent.length;i++)
        {
            for(int j=0;j<sortedPercent.length;j++)
            {
                if(sortedPercent[j]==rankingByPercent[i])
                {
                    // Match the percentages, and store the index, so the the indexes reflect smallest to largest
                    // System.out.println("Swapped "+ sortedPercent[j]+" for " + i);
                    sortedPercent[j]=i;
                    break;
                }
            }
        }
        int ordering []=new int [sortedPercent.length];
        // for(int i=0; i<sortedPercent.length;i++) // debug
        // {
        //     System.out.println("Sorted percent "+i+" Percentage "+sortedPercent[i]);
        //     System.out.println("Ranking By Percent Partition "+sortedPercent[i]+" Percentage " +rankingByPercent[(int)sortedPercent[i]]);
        // }
        for(int i=0; i<=ordering.length-1; i++) // Assign Quadrants for placement of communities
        {
            // ordering[i]=1;i++;
            if(i<ordering.length)
            {
                ordering[i]=1;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=2;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=3;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=4;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=5;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=6;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=7;i++;
            }
            if(i<ordering.length)
            {
                ordering[i]=8;i++;
            }
        }

        // Array is sorted smallest to largest so iterate from largest to smallest for community placement
        for(int i=sortedPercent.length-1; i>=0; i--)
        {
            try
            {
                // partitionFilter.unselectAll();
                // partitionFilter.addPart(partition.getPartFromValue((int)sortedPercent[i])); // Makes a community Active
                //System.out.println("Node Count: "+partitionFilter.getCurrentPartition().getElementsCount());
                //System.out.println("Node Count: "+partitionFilter.getCurrentPartition().getPartsCount());

                // query2 = filterController.createQuery(partitionFilter);
                // view2 = filterController.filter(query2);
                // graphModel.setVisibleView(view2);
                circLayout();
                float commCountFloat=(float)communityCount;
                float percentage=(float) i/commCountFloat;
                // System.out.println("Percentage: "+percentage);
                float placementDegree=percentage*360;
                float placementRadians=(float) (placementDegree*3.14159265359)/180;
                // Dependending on the ordering, nodes are shifted over so they do not overlap,
                // this loop increments the shift multipliers
                if(ordering[i]==1)
                {
                    shifts[0]=shifts[0]+1;
                }
                else if(ordering[i]==2)
                {
                    shifts[1]=shifts[1]+1;
                }
                if(ordering[i]==3)
                {
                    shifts[2]=shifts[2]+1;
                }
                if(ordering[i]==4)
                {
                    shifts[3]=shifts[3]+1;
                }
                if(ordering[i]==5)
                {
                    shifts[4]=shifts[4]+1;
                }
                if(ordering[i]==6)
                {
                    shifts[5]=shifts[5]+1;
                }
                if(ordering[i]==7)
                {
                    shifts[6]=shifts[6]+1;
                }
                if(ordering[i]==8)
                {
                    shifts[7]=shifts[7]+1;
                }
                // Apply shifting to groups of nodes as calculated above.
                for (Node n : graphModel.getGraphVisible().getNodes().toArray())
                {
                    if(ordering[i]==1)
                    {
                        n.setX((n.x()+1000*shifts[0]));
                        n.setY((n.y()+1000*shifts[0]));
                    }
                    else if(ordering[i]==2)
                    {
                        n.setX((n.x()+1000*shifts[1]));
                        n.setY((n.y()-1000*shifts[1]));
                    }
                    else if(ordering[i]==3)
                    {
                        n.setX((n.x()-1000*shifts[2]));
                        n.setY((n.y()-1000*shifts[2]));
                    }
                    else if(ordering[i]==4)
                    {
                        n.setX((n.x()-1000*shifts[3]));
                        n.setY((n.y()+1000*shifts[3]));
                    }
                    else if(ordering[i]==5)
                    {
                        n.setX((n.x()+1000*shifts[4]));
                        n.setY(n.y());
                    }
                    else if(ordering[i]==6)
                    {
                        n.setX(n.x());
                        n.setY(n.y()+1000*shifts[5]);
                    }
                    else if(ordering[i]==7)
                    {
                        n.setX((n.x()-1000*shifts[6]));
                        n.setY(n.y());
                    }
                    else if(ordering[i]==8)
                    {
                        n.setX(n.x());
                        n.setY(n.y()-1000*shifts[7]);
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return;
            }
        }

        // partitionFilter.selectAll(); // Make all circles of nodes reappear

        // query2 = filterController.createQuery(partitionFilter);
        // view2 = filterController.filter(query2);
        // graphModel.setVisibleView(view2);
    }

    // imports graph and appends it to existing graph
    public static void importGraph(File file)
    {
        String fileName = file.getName();

        try
        {
            // Eliminate input extension for use in output name.
            String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
            // String processedFile = tokens[0];

            // Searches file name for a proper date and then appends the date as a time interval
            Matcher m = fileDatePattern.matcher(fileName);
            if (m.find())
            {
                if(isDate(m.group(0)))
                {
                    String date = m.group(0);
                    System.out.println(fileName);
                    System.out.println("Date found: " + m.group(0));

                    // dates[dateCounter] = m.group(1); // Gathers Dates of files to be printed to text file for later use
                    // dateCounter++;
                    // System.out.println("File imported: " + file.toString());
                    Container container = importController.importFile(file);
                    // if(container == null)
                    // {
                    //     System.out.println("Container is null");
                    // }
                    container.getLoader().setEdgeDefault(EdgeDirectionDefault.UNDIRECTED);
                    // container.getLoader().setAllowAutoNode(false);
                    // System.out.println(fileName + " was appended to graph.");
                    // dynamicProcessor.setDate(m.group(1)); // Set time interval
                    // System.out.println("Date Set: " + dynamicProcessor.getDate());
                    importController.process(container, new DefaultProcessor(), workspace);

                    // Set date for this file
                    for(Node node : graph.getNodes())
                    {
                        node.setAttribute("date", date);
                    }

                    // dates[dateCounter]=newDate; // Gathers Dates of files to be printed to text file for later use
                    // dateCounter++;
                    // System.out.println("File imported: " +file.toString());
                    // container = importController.importFile(file);
                    // container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED); // set to undirected
                    // System.out.println(fileName+" was appended to graph.");
                    // dynamicProcessor.setDate(newDate); // Set time interval
                    // System.out.println("Date Set: " +dynamicProcessor.getDate());
                    // // Process the container using the DynamicProcessor
                    // importController.process(container, dynamicProcessor, workspace);
                }
                else
                {
                    System.err.println("Error: File " + fileName + " could not be imported.");
                }
            }
            // NOTE: previously, it was possible to put the date in the file, but this was disabled
            // during the update to a newer version of Gephi, possibly to be re-enabled at a later point
            // else
            // {
            //     // If the Date was not in the file name, attempt to grab date from file
            //     // itself by finding the first three integers in file
            //     System.out.println("Invalid date in File name, searching .dl for date");
            //     int count = 0;
            //     try
            //     {
            //         read = new Scanner(file);
            //     }
            //     catch(Exception ex)
            //     {
            //         ex.printStackTrace();
            //         return;
            //     }

            //     while(read.hasNext())
            //     {
            //         String temp = read.next();
            //         p = Pattern.compile(interiorDate);
            //         m = p.matcher(temp);
            //         if(m.find())
            //         {
            //             count++;
            //             if(count == 1 && temp.length() == 4)
            //             {
            //                 year = temp;
            //             }
            //             else if(count == 2)
            //             {
            //                 // if date is 2008 1 1, must be converted to 2008 01 01 so that it is compatible as time interval
            //                 if(temp.length() == 1)
            //                 {
            //                     month = "0" + temp;
            //                 }
            //                 else
            //                 {
            //                     month = temp;
            //                 }
            //             }
            //             else if(count == 3)
            //             {
            //                 // if date is 2008 1 1, must be converted to 2008 01 01 so that it is compatible as time interval
            //                 if(temp.length() == 1)
            //                 {
            //                     day = "0" + temp;
            //                 }
            //                 else
            //                 {
            //                     day = temp;
            //                 }
            //                 break;
            //             }
            //         }
            //     }

            //     String newDate = year + "-" + month + "-" + day;
            //     System.out.println(newDate);

            //     if(isDate(newDate) == true)
            //     {
            //         // dates[dateCounter] = newDate; // Gathers Dates of files to be printed to text file for later use
            //         // dateCounter++;
            //         // System.out.println("File imported: " + file.toString());
            //         Container container = importController.importFile(file);
            //         container.getLoader().setEdgeDefault(EdgeDefault.UNDIRECTED); // set to undirected
            //         // System.out.println(fileName + " was appended to graph.");
            //         // dynamicProcessor.setDate(newDate); // Set time interval
            //         // System.out.println("Date Set: " + dynamicProcessor.getDate());
            //         // Process the container using the DynamicProcessor
            //         importController.process(container, new DefaultProcessor(), workspace);
            //     }
            //     else
            //     {
            //         System.err.println("Error: File " + fileName + " could not be imported.");
            //     }
            // }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return;
        }
    }

    // Takes in an entire Directory of files and attempts to import each one of them,
    // if the import file is determined to not be a directory, a simple single file import is called.
    public static void importDirOrFile(String path)
    {
        File input = new File(path);
        if(input.isDirectory())
        {
            File[] files = new File(path).listFiles();
            System.out.println("Processing directory: " + path);

            for (File file : files) {
                importGraph(file);
            }
        }
        else if(input.isFile())
        {
            System.out.println("Processing single file: " + path);
            importGraph(input);
        }
    }

    // Exports a gexf file to be used in Gephi or Partiview, exports PDF for easy sample readability of entire graph.
    public static void exportGraph(String dirName, String outputFilename)
    {
        // Set 'show labels' option in Preview - and disable node size influence on text size
        PreviewModel previewModel = Lookup.getDefault().lookup(PreviewController.class).getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE);

        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try
        {
            Path path = ((Paths.get(dirName)).getParent()).getParent();
            // System.out.println("writeGraphOut " + path.toString()); // processedFile +" "
            // ec.exportFile(new File("completeLayout.gexf"));
            // ec.exportFile(new File(path.toString()+"/partiview_generator/"+processedFile+".pdf"));
            // ec.exportFile(new File(path.toString()+"/partiview_generator/"+processedFile+".gexf"));
            ec.exportFile(new File(path.toString()+"/partiview_generator/"+outputFilename+".pdf"));
            ec.exportFile(new File(path.toString()+"/partiview_generator/"+outputFilename+".gexf"));
            // ec.exportFile(new File(processedFile+".pdf"));
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return;
        }
    }

    // Exports the discrete date of each file in a .txt file to be utilized later in Partiview
    // to discretize time intervals.
    public static void exportDates()
    {
        try
        {
            PrintWriter pr = new PrintWriter("FileDates.txt");

            for (int i=0; i<dates.length; i++)
            {
                if(dates[i]!= null)
                {
                    pr.println(dates[i]);
                }
            }
            pr.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("No such file exists.");
        }
    }

    public static boolean isDate(String dateIn)
    {
        boolean isDate;
        int year;
        int month;
        int day;
        String hyphen1;
        String hyphen2;
        try
        {
            year = Integer.parseInt(dateIn.substring(0,4));
            if(year<=0 || year >2050)
            {
                System.out.println("No way a file has been created with that date yet");
                return isDate = false;
            }
            hyphen1=dateIn.substring(4,5);
            month = Integer.parseInt(dateIn.substring(5, 7));
            if(month<=0 || month >12)
            {
                System.out.println("Not a Month");
                return isDate = false;
            }
            hyphen2=dateIn.substring(7,8);
            day = Integer.parseInt(dateIn.substring(8, 10));
            if(day<=0 || day >31)
            {
                System.out.println("Out of range of days in months");
                return isDate = false;
            }
            isDate = true;
        }
        catch(Exception e)
        {
            isDate = false;
        }
        return isDate;
    }

    public static String getSizeNodesBy()
    {
        return sizeNodesBy;
    }
    public static void setSizeNodesBy(String size)
    {
        sizeNodesBy = size;
    }
    public static double getModResolution()
    {
        return modResolution;
    }
    public static void setModResolution(double resIn)
    {
        if(resIn > 0 && resIn <= 1.0)
        {
            modResolution=resIn;
        }
        else // reset to default
        {
            modResolution=0.4;
        }
    }

    public static void testExport()
    {
        //Export full graph
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("test.gexf"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
