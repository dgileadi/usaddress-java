package io.github.dgileadi.usaddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.github.dgileadi.usaddress.Address.Field;
import io.github.dgileadi.usaddress.Address.FieldType;

class TestLabeling {
    private static final Pattern STRIP_STRING_PATTERN = Pattern.compile("[\\s,;]+$|^[\\s,;]+");
    private static final Pattern AMPERSAND_PATTERN = Pattern.compile("(&#38;)|(&amp;)");

    @Test
    void testAddress() {
        Address address = AddressParser.parseAndClean("1775 Broadway And 57th, Newyork NY");

        assertEquals(Address.Type.AMBIGUOUS, address.getType());

        List<Field> fields = address.getFields();
        assertNotNull(fields);
        assertEquals(6, fields.size());

        Address.Field field = fields.get(0);
        assertEquals(Address.FieldType.ADDRESS_NUMBER, field.getType());
        assertEquals("1775", field.getValue());

        field = fields.get(1);
        assertEquals(Address.FieldType.STREET_NAME, field.getType());
        assertEquals("Broadway", field.getValue());

        field = fields.get(2);
        assertEquals(Address.FieldType.INTERSECTION_SEPARATOR, field.getType());
        assertEquals("And", field.getValue());

        field = fields.get(3);
        assertEquals(Address.FieldType.SECOND_STREET_NAME, field.getType());
        assertEquals("57th", field.getValue());

        field = fields.get(4);
        assertEquals(Address.FieldType.PLACE_NAME, field.getType());
        assertEquals("Newyork", field.getValue());

        field = fields.get(5);
        assertEquals(Address.FieldType.STATE_NAME, field.getType());
        assertEquals("NY", field.getValue());
    }

    @Test
    void testSimpleAddresses() throws ParserConfigurationException, SAXException, IOException {
        testAddresses("simple_address_patterns.xml", true);
    }

    @Test
    void testSyntheticOSM() throws ParserConfigurationException, SAXException, IOException {
        testAddresses("synthetic_clean_osm_data.xml", true,
                "5240;5220 3rd Street, 93432",
                "2303;2253 Sheridan Road, Evanston, 60208",
                "4922-B South Cornell Avenue, 60615",
                "9010 West Front Road, atascadero, 93422",
                "214 B South Boulevard, Evanston, 60202",
                "758 West Chicago Avenue, Chicago, IL",
                "1501A Chicago Avenue, Evanston, 60201",
                "6257A North McCormick Road, Chicago, 60659");
    }

    @Test
    void testUS50() throws ParserConfigurationException, SAXException, IOException {
        testAddresses("us50_test_tagged.xml", false);
    }

    @Test
    void testLabeled() throws ParserConfigurationException, SAXException, IOException {
        testAddresses("labeled.xml", false,
                "5418 RIVER RD LIBERTY GROVE RD NORTH WILKESBORO 28659",
                "0 E WARDELL DR # APT 3 PEMBROKE 28372",
                "406 North Highway 71 Business Lowell AR 72745",
                "2500 S AND W FARM RD HICKORY 28602",
                "4483 MANHATTAN COLLEGE PY",
                "333 STATE ROUTE 590 ROARING BROOK TWP, PA 18444",
                "900 Business 150 STE 3, Mansfield, PA 19402");
    }

    void testAddresses(final String xmlResource, final boolean clean, final String... expectedMismatches)
            throws ParserConfigurationException, SAXException, IOException {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlResource);
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(input));
        doc.normalize();

        NodeList addressStrings = doc.getDocumentElement().getElementsByTagName("AddressString");
        for (int i = 0; i < addressStrings.getLength(); i++) {
            Element addressElement = (Element) addressStrings.item(i);
            testAddress(addressElement, clean, expectedMismatches);
        }
    }

    private void testAddress(final Element addressElement, final boolean clean, final String[] expectedMismatches) {
        String addressText = addressElement.getTextContent();
        boolean expectFailure = Arrays.stream(expectedMismatches)
                .anyMatch(expectedMismatch -> expectedMismatch.equals(addressText));
        boolean failed = false;

        Address address = clean ? AddressParser.parseAndClean(addressText)
                : AddressParser.parse(addressText);

        Node node = getNextElement(addressElement.getFirstChild());

        try {
            for (Field field : address.getFields()) {
                assertNotNull(node, "Expected a tag to compare to for address <" + addressText + ">");

                String expectedType = node.getNodeName();
                String expectedValue = AMPERSAND_PATTERN.matcher(node.getTextContent()).replaceAll("&");

                Node nextNode = getNextElement(node.getNextSibling());

                if (clean) {
                    while (nextNode != null && nextNode.getNodeName().equals(expectedType)) {
                        expectedValue = expectedValue + " " + nextNode.getTextContent();
                        nextNode = getNextElement(nextNode.getNextSibling());
                    }
                    expectedValue = STRIP_STRING_PATTERN.matcher(expectedValue).replaceAll("");
                }

                FieldType actualType = field.getType();
                String actualValue = field.getValue();

                // allow fuzzy matching for modifiers
                for (String label : new String[] { "StreetName", "AddressNumber" }) {
                    if (label.equals(expectedType) && actualType.getLabel().startsWith(label)) {
                        expectedType = actualType.getLabel();
                    }
                }

                assertNotNull(node, "Expected a tag to compare to in address <" + addressText + ">");
                assertEquals(expectedType, actualType.getLabel(),
                        "Parsed as a different label for field <" + actualValue + "> in address <"
                                + addressText + ">");
                assertEquals(expectedValue, actualValue,
                        "Different text content in field <" + expectedType + "> of address <"
                                + addressText + ">");

                node = nextNode;
            }

            assertNull(node, "Shouldn't have had any more expected tags for address <" + addressText + ">");
        } catch (AssertionFailedError e) {
            failed = true;
            if (expectFailure) {
                System.out.println("Found an expected parsing mismatch: " + e.getMessage());
            } else {
                throw e;
            }
        }

        assertEquals(expectFailure, failed,
                "Should have failed to successfully parse address <" + addressText + ">");
    }

    private Node getNextElement(Node node) {
        while (node != null && !(node instanceof Element)) {
            node = node.getNextSibling();
        }
        return node;
    }

}
