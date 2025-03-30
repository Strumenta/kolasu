package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.DebugPrintConfiguration;
import com.strumenta.kolasu.model.BaseASTNode;
import com.strumenta.kolasu.model.PrintingKt;

/**
 * This class permits to print nodes, using a custom configuration
 */
public class DebugPrinter {

    private DebugPrintConfiguration configuration = new DebugPrintConfiguration();

    public DebugPrintConfiguration getConfiguration() {
        return this.configuration;
    }

    public String printNodeToString(BaseASTNode node) {
        return PrintingKt.debugPrint(node, "", configuration);
    }

    public void printNodeOnConsole(BaseASTNode node) {
        System.out.println(printNodeToString(node));
    }
}
