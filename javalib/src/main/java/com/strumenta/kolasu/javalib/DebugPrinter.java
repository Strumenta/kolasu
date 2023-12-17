package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.DebugPrintConfiguration;
import com.strumenta.kolasu.model.INode;
import com.strumenta.kolasu.model.PrintingKt;

/**
 * This class permits to print nodes, using a custom configuration
 */
public class DebugPrinter {

    private DebugPrintConfiguration configuration = new DebugPrintConfiguration();

    public DebugPrintConfiguration getConfiguration() {
        return this.configuration;
    }

    public String printNodeToString(INode node) {
        return PrintingKt.debugPrint(node, "", configuration);
    }

    public void printNodeOnConsole(INode node) {
        System.out.println(printNodeToString(node));
    }
}
