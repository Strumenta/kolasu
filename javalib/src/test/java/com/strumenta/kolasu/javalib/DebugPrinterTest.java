package com.strumenta.kolasu.javalib;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DebugPrinterTest {

    @Test
    public void testSimpleCase() {
        Library root = new Library(Arrays.asList(
                new Book("Effective Kolasu", 50),
                new Book("Extraordinaty Pylasu", 40),
                new Book("Magic Tylasu", 140)
        ));
        DebugPrinter debugPrinter = new DebugPrinter();
        assertEquals("Library {\n" +
                "  books = [\n" +
                "    Book {\n" +
                "      numberOfPages = 50\n" +
                "      title = Effective Kolasu\n" +
                "    } // Book\n" +
                "    Book {\n" +
                "      numberOfPages = 40\n" +
                "      title = Extraordinaty Pylasu\n" +
                "    } // Book\n" +
                "    Book {\n" +
                "      numberOfPages = 140\n" +
                "      title = Magic Tylasu\n" +
                "    } // Book\n" +
                "  ]\n" +
                "} // Library\n", debugPrinter.printNodeToString(root));
    }
}
