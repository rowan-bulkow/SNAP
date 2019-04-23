#!/bin/bash

#
# Builds and runs the GephiPipe application.
# Runs in same directory as java files.
# Outputs to testOutput/ directory.
# Requires gephi-toolkit.jar and org-gephi-layout-plugin-circularlayout.jar in lib/ directory:
# - build.sh
# - AutoGephiPipe.java
# - Main.java
# - lib
#   - gephi-toolkit.jar
#   - org-gephi-layout-plugin-circularlayout.jar
# - testOutput
#   - test.gexf
#
# Usage
#
# build.sh [command] [params]
#
# Where command is one of the following:
#
# run - runs gephi-pipe.jar, requires input path, allows params: layout, modResolution, sizeMetric
#   build.sh run inputPath [layout modResolution sizeMetric]
#
# build - compiles java files, bundles into gephi-pipe.jar, does not take any params
#   build.sh build
#
# buildandrun - builds, then runs. Requires and allows the same arguments that run does.
#   build.sh buildandrun inputPath [layout modResolution]
#
# Params:
# inputPath - folder containing input .dl files
# layout - desired layout for graph. Default: 0, Circular Star Layout. Possible values:
#     0 - Circular star layout
#     1 - Radial axis layout
#     2 - Yifan Hu layout
#     3 - Force Atlas layout
# modResolution - desired mod resolution. Default: 0.4
# sizeMetric - desired metric for resizing. Default: betweenness. Possible values:
#     betweenness
#     closeness
#     degree
#

build() {
    echo "Building..."
    javac -cp .:lib/gephi-toolkit.jar:lib/org-gephi-layout-plugin-circularlayout.jar *.java
    if [[ $? == 0 ]]; then
        sudo jar cfmv ./gephi-pipe.jar MANIFEST.MF *.class
        if [[ $? == 0 ]]; then
            echo "Successfully built."
            return 0
        fi
    fi
    echo "Error: Problem building."
    return 1
}

run() {
    java -jar gephi-pipe.jar $1 $2 $3 $4
    if [[ $? == 0 ]]; then
        return 0
    fi
    echo "Error: Problem running."
    return 1
}

command=$1

if [[ "$command" == "run" ]]; then
    if [[ $# < 2 || $# > 5 ]]; then
        echo "Error: Invalid number of arguments for run."
        exit 1
    fi

    input=$2
    layout="0"
    modResolution="0.4"
    sizeMetric="betweenness"
    if [[ $# == 3 ]]; then
        layout=$3
    elif [[ $# == 4 ]]; then
        layout=$3
        modResolution=$4
    elif [[ $# == 5 ]]; then
        layout=$3
        modResolution=$4
        sizeMetric=$5
    fi
    run $input $layout $modResolution $sizeMetric
elif [[ "$command" == "build" ]]; then
    build
    if [[ $? != 0 ]]; then
        exit 1
    fi
elif [[ "$command" == "buildandrun" ]]; then
    build
    if [[ $? != 0 ]]; then
        echo "Error: Problem building, cancelling run."
        exit 1
    fi

    if [[ $# < 2 || $# > 5 ]]; then
        echo "Error: Invalid number of arguments for run."
        exit 1
    fi

    input=$2
    layout="0"
    modResolution="0.4"
    sizeMetric="betweenness"
    if [[ $# == 3 ]]; then
        layout=$3
    elif [[ $# == 4 ]]; then
        layout=$3
        modResolution=$4
    elif [[ $# == 5 ]]; then
        layout=$3
        modResolution=$4
        sizeMetric=$5
    fi
    run $input $layout $modResolution $sizeMetric
else
    echo "Error: Command not recognized."
fi
