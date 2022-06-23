package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.DebugPrintConfiguration;
import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.model.PrintingKt;

/**
 * This class permits to print nodes, using a custom configuration
 */
public class DebugPrinter {

    private DebugPrintConfiguration configuration = new DebugPrintConfiguration();

    public DebugPrintConfiguration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(DebugPrintConfiguration debugPrintConfiguration) {
        this.configuration = debugPrintConfiguration;
    }

    public String printNodeToString(Node node) {
        return PrintingKt.debugPrint(node, "", configuration);
    }

    public void printNodeOnConsole(Node node) {
        System.out.println(printNodeToString(node));
    }
}
