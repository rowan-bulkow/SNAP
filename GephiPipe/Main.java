//
// Copyright Martin Cenek <drcenek@gmail.com> 2016-2019
//
// All source code is released under the terms of the MIT License.
// See LICENSE for more information.
// Contributions from: 
// Eric Pak, Levi Oyster, Boyd Ching, Rowan Bulkow, Neal Logan, Mackenzie Bartlett
//
//package AutoGephi;

import java.io.IOException;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Main
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        System.out.println("AutoGephiPipe:");

        System.out.println("Args received: ");
        for(int i=0; i < args.length; i++) {
            System.out.println(String.format("args[%d]=%s", i, args[i]));
        }

        if(args.length != 3) {
            System.err.println("Incorrect number of arguments (Expected 3).");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        String inputLayout = args[1];
        Double inputModResolution = Double.parseDouble(args[2]);

        if(args.length == 3) {
            try(Stream<Path> paths = Files.walk(inputPath)) {
                paths.forEach(filePath -> {
                    if(Files.isRegularFile(filePath)) {
                        System.out.println(filePath.toString());

                        if(filePath.toString().endsWith(".dl")) {
                            AutoGephiPipe.initialize();

                            AutoGephiPipe.importDirectory(filePath.toString());

                            AutoGephiPipe.setModResolution(inputModResolution);

                            AutoGephiPipe.setSizeNodesBy("Betweenness");

                            AutoGephiPipe.sizeNodes();

                            AutoGephiPipe.colorByCommunity();

                            switch (inputLayout) {
                                case AutoGephiPipe.CIRCULAR_STAR_LAYOUT:
                                    AutoGephiPipe.circularStarLayout();
                                    break;
                                case AutoGephiPipe.RADIAL_AXIS_LAYOUT:
                                    AutoGephiPipe.radialAxLayout();
                                    break;
                                case AutoGephiPipe.YIFAN_HU_LAYOUT:
                                    AutoGephiPipe.yifanHuLayout();
                                    break;
                                case AutoGephiPipe.FORCE_ATLAS_LAYOUT:
                                    AutoGephiPipe.forceAtlasLayout();
                                    break;
                                default:
                                    System.err.println("Error: Invalid layout");
                                    System.exit(1);
                                    break;
                            }

                            AutoGephiPipe.exportGraph(filePath.toString());
                            //AutoGephiPipe.exportDates();
                        }
                    }
                });
            } catch(Exception e) {
                System.err.println("Error processing dl files.");
            }
        } else {
            System.err.println("Incorrect number of arguments (Expected 3).");
        }
    }
}
