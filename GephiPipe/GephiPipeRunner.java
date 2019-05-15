//
// Copyright Martin Cenek <drcenek@gmail.com> 2016-2019
//
// All source code is released under the terms of the MIT License.
// See LICENSE for more information.
// Contributions from:
// Eric Pak, Levi Oyster, Boyd Ching, Rowan Bulkow, Neal Logan, Mackenzie Bartlett
//

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

// Converts a folder of .dl files and converts them, one by one, into .gexf files
// using the specified parameters (all 4 required):
// - inputPath (path to folder of .dl files)
// - inputLayout (int, 0-3)
// - inputModResolution (double)
// - sizeMetric (string)
//
// Where inputLayout is one of:
// 0 - Circular Star Layout
// 1 - Radial Axis Layout
// 2 - Yifan Hu Layout
// 3 - Force Atlas Layout
//
// Where sizeMetric is one of:
// betweenness
// closeness
// degree
//
// The output .gexf files are placed in the same folder as the input .dl files.
public class GephiPipeRunner {
    public static void main(String[] args) throws IOException {
        System.out.println("GephiPipeRunner:");

        System.out.println("Args received: ");
        for (int i = 0; i < args.length; i++) {
            System.out.println(String.format("args[%d]=%s", i, args[i]));
        }

        if (args.length != 4) {
            System.err.println("Incorrect number of arguments (Expected 4).");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        String inputLayout = args[1];
        Double inputModResolution = Double.parseDouble(args[2]);
        String sizeMetric = args[3];

        try (Stream<Path> paths = Files.walk(inputPath)) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().endsWith(".dl")) {
                        AutoGephiPipe.initialize();

                        AutoGephiPipe.importDirOrFile(filePath.toString());

                        AutoGephiPipe.setModResolution(inputModResolution);

                        AutoGephiPipe.setSizeNodesBy(sizeMetric);

                        AutoGephiPipe.sizeNodes();

                        AutoGephiPipe.colorByCommunity();

                        System.out.println("Colored by community, moving to layout");

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

                        System.out.println("Uhh exporting?");

                        String outName = FilenameUtils.getBaseName(filePath.toString());
                        AutoGephiPipe.exportGraph(inputPath.toString(), outName);
                        // AutoGephiPipe.testExport();
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error processing dl files: " + e.getMessage());
            System.exit(1);
        }
    }
}
