package io.github.dgileadi.usaddress;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TestTokenizing {

    @Test
    void testHash() {
        assertIterableEquals(
                AddressParser.tokenize("# 1 abc st"),
                List.of("#", "1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("#1 abc st"),
                List.of("#", "1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("box # 1 abc st"),
                List.of("box", "#", "1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("box #1 abc st"),
                List.of("box", "#", "1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("box# 1 abc st"),
                List.of("box", "#", "1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("box#1 abc st"),
                List.of("box", "#", "1", "abc", "st"));
    }

    @Test
    void testSplitOnPunc() {
        assertIterableEquals(
                AddressParser.tokenize("1 abc st,suite 1"),
                List.of("1", "abc", "st,", "suite", "1"));
        assertIterableEquals(
                AddressParser.tokenize("1 abc st;suite 1"),
                List.of("1", "abc", "st;", "suite", "1"));
        assertIterableEquals(
                AddressParser.tokenize("1-5 abc road"),
                List.of("1-5", "abc", "road"));
    }

    @Test
    void testSpaces() {
        assertIterableEquals(
                AddressParser.tokenize("1 abc st"),
                List.of("1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("1  abc st"),
                List.of("1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize("1 abc st "),
                List.of("1", "abc", "st"));
        assertIterableEquals(
                AddressParser.tokenize(" 1 abc st"),
                List.of("1", "abc", "st"));
    }

    @Test
    void testCapturePunc() {
        assertIterableEquals(
                AddressParser.tokenize("222 W. Merchandise Mart Plaza"),
                List.of("222", "W.", "Merchandise", "Mart", "Plaza"));
        assertIterableEquals(
                AddressParser.tokenize("222 W Merchandise Mart Plaza, Chicago, IL"),
                List.of("222", "W", "Merchandise", "Mart", "Plaza,", "Chicago,", "IL"));
        assertIterableEquals(
                AddressParser.tokenize("123 Monroe- St"),
                List.of("123", "Monroe-", "St"));
    }

    @Test
    void testNums() {
        assertIterableEquals(
                AddressParser.tokenize("222 W Merchandise Mart Plaza Chicago IL 60654"),
                List.of("222", "W", "Merchandise", "Mart", "Plaza", "Chicago", "IL", "60654"));
    }

    @Test
    void testAmpersand() {
        assertIterableEquals(
                AddressParser.tokenize("123 & 456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123&456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123& 456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123 &456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123 &#38; 456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123&#38;456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123&#38; 456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123 &#38;456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123 &amp; 456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123&amp;456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123&amp; 456"),
                List.of("123", "&", "456"));
        assertIterableEquals(
                AddressParser.tokenize("123 &amp;456"),
                List.of("123", "&", "456"));
    }

    @Test
    void testParen() {
        assertIterableEquals(
                AddressParser.tokenize("222 W Merchandise Mart Plaza (1871) Chicago IL 60654"),
                List.of("222", "W", "Merchandise", "Mart", "Plaza", "(1871)", "Chicago", "IL", "60654"));
        assertIterableEquals(
                AddressParser.tokenize("222 W Merchandise Mart Plaza (1871), Chicago IL 60654"),
                List.of("222", "W", "Merchandise", "Mart", "Plaza", "(1871),", "Chicago", "IL", "60654"));
        assertIterableEquals(
                AddressParser.tokenize("222 W Merchandise Mart Plaza(1871) Chicago IL 60654"),
                List.of("222", "W", "Merchandise", "Mart", "Plaza", "(1871)", "Chicago", "IL", "60654"));
    }

}
