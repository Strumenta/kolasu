package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.DebugPrintConfiguration;
import com.strumenta.kolasu.ast.NodeLike;
import com.strumenta.kolasu.model.PrintingKt;

/**
 * This class permits to print nodes, using a custom configuration
 */
public class DebugPrinter {

    private DebugPrintConfiguration configuration = new DebugPrintConfiguration();

    public DebugPrintConfiguration getConfiguration() {
        return this.configuration;
    }

    public String printNodeToString(NodeLike node) {
        return PrintingKt.debugPrint(node, "", configuration);
    }

    public void printNodeOnConsole(NodeLike node) {
        System.out.println(printNodeToString(node));
    }
}
