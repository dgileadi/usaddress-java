package io.github.dgileadi.usaddress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gengoai.jcrfsuite.CrfTagger;
import com.gengoai.jcrfsuite.util.Pair;

import third_party.org.chokkan.crfsuite.Attribute;
import third_party.org.chokkan.crfsuite.Item;
import third_party.org.chokkan.crfsuite.ItemSequence;

/**
 * Parse US addresses into fields and detect the type of each field.
 */
public final class AddressParser {
    private static final Pattern AMPERSAND_PATTERN = Pattern.compile("(&#38;)|(&amp;)");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\(*\\b[^\\s,;#&\\(\\)]+[.,;\\)\\n]*|[#&]",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern CLEAN_TOKEN_PATTERN = Pattern.compile("(^[\\W]*)|([^.\\w]*$)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private static final Pattern TOKEN_ENDS_IN_PUNC_PATTERN = Pattern.compile(".+[^.\\w]",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern VOWELS_PATTERN = Pattern.compile("[aeiou]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_ZEROS_PATTERN = Pattern.compile("(0+)$");

    private static final Set<String> DIRECTIONS = new HashSet<>(Arrays.asList(
            "n", "s", "e", "w",
            "ne", "nw", "se", "sw",
            "north", "south", "east", "west",
            "northeast", "northwest", "southeast", "southwest"));

    private static final Set<String> STREET_NAMES = new HashSet<>(Arrays.asList(
            "allee", "alley", "ally", "aly", "anex", "annex", "annx", "anx",
            "arc", "arcade", "av", "ave", "aven", "avenu", "avenue", "avn", "avnue",
            "bayoo", "bayou", "bch", "beach", "bend", "bg", "bgs", "bl", "blf",
            "blfs", "bluf", "bluff", "bluffs", "blvd", "bnd", "bot", "bottm",
            "bottom", "boul", "boulevard", "boulv", "br", "branch", "brdge", "brg",
            "bridge", "brk", "brks", "brnch", "brook", "brooks", "btm", "burg",
            "burgs", "byp", "bypa", "bypas", "bypass", "byps", "byu", "camp", "canyn",
            "canyon", "cape", "causeway", "causwa", "causway", "cen", "cent",
            "center", "centers", "centr", "centre", "ci", "cir", "circ", "circl",
            "circle", "circles", "cirs", "ck", "clb", "clf", "clfs", "cliff",
            "cliffs", "club", "cmn", "cmns", "cmp", "cnter", "cntr", "cnyn", "common",
            "commons", "cor", "corner", "corners", "cors", "course", "court",
            "courts", "cove", "coves", "cp", "cpe", "cr", "crcl", "crcle", "crecent",
            "creek", "cres", "crescent", "cresent", "crest", "crk", "crossing",
            "crossroad", "crossroads", "crscnt", "crse", "crsent", "crsnt", "crssing",
            "crssng", "crst", "crt", "cswy", "ct", "ctr", "ctrs", "cts", "curv",
            "curve", "cv", "cvs", "cyn", "dale", "dam", "div", "divide", "dl", "dm",
            "dr", "driv", "drive", "drives", "drs", "drv", "dv", "dvd", "est",
            "estate", "estates", "ests", "ex", "exp", "expr", "express", "expressway",
            "expw", "expy", "ext", "extension", "extensions", "extn", "extnsn",
            "exts", "fall", "falls", "ferry", "field", "fields", "flat", "flats",
            "fld", "flds", "fls", "flt", "flts", "ford", "fords", "forest", "forests",
            "forg", "forge", "forges", "fork", "forks", "fort", "frd", "frds",
            "freeway", "freewy", "frg", "frgs", "frk", "frks", "frry", "frst", "frt",
            "frway", "frwy", "fry", "ft", "fwy", "garden", "gardens", "gardn",
            "gateway", "gatewy", "gatway", "gdn", "gdns", "glen", "glens", "gln",
            "glns", "grden", "grdn", "grdns", "green", "greens", "grn", "grns",
            "grov", "grove", "groves", "grv", "grvs", "gtway", "gtwy", "harb",
            "harbor", "harbors", "harbr", "haven", "havn", "hbr", "hbrs", "height",
            "heights", "hgts", "highway", "highwy", "hill", "hills", "hiway", "hiwy",
            "hl", "hllw", "hls", "hollow", "hollows", "holw", "holws", "hrbor", "ht",
            "hts", "hvn", "hway", "hwy", "inlet", "inlt", "is", "island", "islands",
            "isle", "isles", "islnd", "islnds", "iss", "jct", "jction", "jctn",
            "jctns", "jcts", "junction", "junctions", "junctn", "juncton", "key",
            "keys", "knl", "knls", "knol", "knoll", "knolls", "ky", "kys", "la",
            "lake", "lakes", "land", "landing", "lane", "lanes", "lck", "lcks", "ldg",
            "ldge", "lf", "lgt", "lgts", "light", "lights", "lk", "lks", "ln", "lndg",
            "lndng", "loaf", "lock", "locks", "lodg", "lodge", "loop", "loops", "lp",
            "mall", "manor", "manors", "mdw", "mdws", "meadow", "meadows", "medows",
            "mews", "mi", "mile", "mill", "mills", "mission", "missn", "ml", "mls",
            "mn", "mnr", "mnrs", "mnt", "mntain", "mntn", "mntns", "motorway",
            "mount", "mountain", "mountains", "mountin", "msn", "mssn", "mt", "mtin",
            "mtn", "mtns", "mtwy", "nck", "neck", "opas", "orch", "orchard", "orchrd",
            "oval", "overlook", "overpass", "ovl", "ovlk", "park", "parks", "parkway",
            "parkways", "parkwy", "pass", "passage", "path", "paths", "pike", "pikes",
            "pine", "pines", "pk", "pkway", "pkwy", "pkwys", "pky", "pl", "place",
            "plain", "plaines", "plains", "plaza", "pln", "plns", "plz", "plza",
            "pne", "pnes", "point", "points", "port", "ports", "pr", "prairie",
            "prarie", "prk", "prr", "prt", "prts", "psge", "pt", "pts", "pw", "pwy",
            "rad", "radial", "radiel", "radl", "ramp", "ranch", "ranches", "rapid",
            "rapids", "rd", "rdg", "rdge", "rdgs", "rds", "rest", "ri", "ridge",
            "ridges", "rise", "riv", "river", "rivr", "rn", "rnch", "rnchs", "road",
            "roads", "route", "row", "rpd", "rpds", "rst", "rte", "rue", "run", "rvr",
            "shl", "shls", "shoal", "shoals", "shoar", "shoars", "shore", "shores",
            "shr", "shrs", "skwy", "skyway", "smt", "spg", "spgs", "spng", "spngs",
            "spring", "springs", "sprng", "sprngs", "spur", "spurs", "sq", "sqr",
            "sqre", "sqrs", "sqs", "squ", "square", "squares", "st", "sta", "station",
            "statn", "stn", "str", "stra", "strav", "strave", "straven", "stravenue",
            "stravn", "stream", "street", "streets", "streme", "strm", "strt",
            "strvn", "strvnue", "sts", "sumit", "sumitt", "summit", "te", "ter",
            "terr", "terrace", "throughway", "tl", "tpk", "tpke", "tr", "trace",
            "traces", "track", "tracks", "trafficway", "trail", "trailer", "trails",
            "trak", "trce", "trfy", "trk", "trks", "trl", "trlr", "trlrs", "trls",
            "trnpk", "trpk", "trwy", "tunel", "tunl", "tunls", "tunnel", "tunnels",
            "tunnl", "turn", "turnpike", "turnpk", "un", "underpass", "union",
            "unions", "uns", "upas", "valley", "valleys", "vally", "vdct", "via",
            "viadct", "viaduct", "view", "views", "vill", "villag", "village",
            "villages", "ville", "villg", "villiage", "vis", "vist", "vista", "vl",
            "vlg", "vlgs", "vlly", "vly", "vlys", "vst", "vsta", "vw", "vws", "walk",
            "walks", "wall", "way", "ways", "well", "wells", "wl", "wls", "wy", "xc",
            "xg", "xing", "xrd", "xrds"));

    private static final String MODEL_FILE = "usaddr.crfsuite";
    private static CrfTagger tagger;

    static {
        try {
            InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(MODEL_FILE);
            File tempFile = File.createTempFile(MODEL_FILE, null);
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tagger = new CrfTagger(tempFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Error copying usaddr.crfsuite to a temporary file", e);
        }
    }

    private AddressParser() {
    }

    /**
     * Split an address string into fields, and assign a type to each field.
     * Also merge consecutive fields and strip commas.
     *
     * @param address the address string to parse.
     * @return the parsed address.
     */
    public static Address parseAndClean(final String address) {
        List<Address.Field> fields = new ArrayList<>();

        Address.FieldType lastType = null;
        boolean isIntersection = false;

        for (Address.Field field : parseFields(address)) {
            String token = field.getValue();
            Address.FieldType type = field.getType();

            if (type == Address.FieldType.INTERSECTION_SEPARATOR) {
                isIntersection = true;
            } else if (type == Address.FieldType.STREET_NAME && isIntersection) {
                field = new Address.Field(field.getValue(), Address.FieldType.SECOND_STREET_NAME);
            }

            if (type == lastType) {
                fields.get(fields.size() - 1).appendValue(token);
            } else {
                fields.add(field);
            }

            lastType = type;
        }

        for (Address.Field field : fields) {
            field.cleanValue();
        }

        return new Address(detectType(fields), fields);
    }

    /**
     * Split an address string into fields, and assign a type to each field.
     *
     * @param address the address string to parse.
     * @return the parsed address.
     */
    public static Address parse(final String address) {
        List<Address.Field> fields = parseFields(address);
        return new Address(detectType(fields), fields);
    }

    private static List<Address.Field> parseFields(final String address) {
        List<String> tokens = tokenize(address);
        ItemSequence features = tokens2features(tokens);
        // Uncomment this line to print the features sent to the tagger:
        // printItemSequence(features);
        List<Pair<String, Double>> tags = tagger.tag(features);

        List<Address.Field> result = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            result.add(new Address.Field(tokens.get(i), tags.get(i).getFirst()));
        }
        return result;
    }

    private static Address.Type detectType(final List<Address.Field> fields) {
        boolean hasAddressNumber = fields.stream()
                .anyMatch(field -> field.getType() == Address.FieldType.ADDRESS_NUMBER);
        boolean isIntersection = fields.stream()
                .anyMatch(field -> field.getType() == Address.FieldType.INTERSECTION_SEPARATOR);

        if (hasAddressNumber && !isIntersection) {
            return Address.Type.STREET_ADDRESS;
        } else if (isIntersection && !hasAddressNumber) {
            return Address.Type.INTERSECTION;
        } else if (fields.stream().anyMatch(field -> field.getType() == Address.FieldType.USPS_BOX_ID)) {
            return Address.Type.PO_BOX;
        } else {
            return Address.Type.AMBIGUOUS;
        }
    }

    // package protected to support testing
    static List<String> tokenize(String address) {
        address = AMPERSAND_PATTERN.matcher(address).replaceAll("&");
        Matcher matcher = TOKEN_PATTERN.matcher(address);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static Item tokenFeatures(final String token) {
        String cleanToken;
        if ("&".equals(token) || "#".equals(token) || "½".equals(token)) {
            cleanToken = token;
        } else {
            cleanToken = CLEAN_TOKEN_PATTERN.matcher(token).replaceAll("");
        }

        String tokenAbbrev = cleanToken.toLowerCase(Locale.US).replace(".", "");
        Item features = new Item();
        features.add(new Attribute("abbrev", cleanToken.endsWith(".") ? 1 : 0));
        features.add(new Attribute("digits:" + digits(cleanToken), 1));
        if (DIGITS_PATTERN.matcher(tokenAbbrev).matches()) {
            features.add(new Attribute("word", 0));
            features.add(new Attribute("trailing.zeros:" + trailingZeros(tokenAbbrev), 1));
            features.add(new Attribute("length:d:" + tokenAbbrev.length(), 1));
        } else {
            features.add(new Attribute("word:" + tokenAbbrev, 1));
            features.add(new Attribute("trailing.zeros", 0));
            features.add(new Attribute("length:w:" + tokenAbbrev.length(), 1));
        }
        if (TOKEN_ENDS_IN_PUNC_PATTERN.matcher(token).matches()) {
            features.add(new Attribute("endsinpunc:" + token.charAt(token.length() - 1), 1));
        } else {
            features.add(new Attribute("endsinpunc", 0));
        }
        features.add(new Attribute("directional", DIRECTIONS.contains(tokenAbbrev) ? 1 : 0));
        features.add(new Attribute("street_name", STREET_NAMES.contains(tokenAbbrev) ? 1 : 0));
        features.add(new Attribute("has.vowels", VOWELS_PATTERN.matcher(tokenAbbrev.substring(1)).find() ? 1 : 0));

        return features;
    }

    private static ItemSequence tokens2features(final List<String> address) {
        ItemSequence featureSequence = new ItemSequence();
        Item previousFeatures = null;

        int i = 0;
        for (String token : address) {
            Item features = tokenFeatures(token);
            Item currentFeatures = copy(features);

            if (i > 0) {
                addAttributes("previous", previousFeatures, features);
                if (i == 1) {
                    features.add(new Attribute("previous:address.start", 1));
                }
                addAttributes("next", currentFeatures, featureSequence.get(i - 1));
            }

            featureSequence.add(features);

            previousFeatures = currentFeatures;
            ++i;
        }

        featureSequence.get(0).add(new Attribute("address.start", 1));
        featureSequence.get((int) featureSequence.size() - 1).add(new Attribute("address.end", 1));
        if (featureSequence.size() > 1) {
            featureSequence.get((int) featureSequence.size() - 2).add(new Attribute("next:address.end", 1));
        }

        return featureSequence;
    }

    private static Item copy(final Item source) {
        Item copy = new Item();
        for (int i = 0; i < source.size(); i++) {
            copy.add(source.get(i));
        }
        return copy;
    }

    private static void addAttributes(final String prefix, final Item from, final Item into) {
        for (int i = 0; i < from.size(); i++) {
            Attribute attribute = from.get(i);
            into.add(new Attribute(prefix + ":" + attribute.getAttr(), attribute.getValue()));
        }
    }

    private static String digits(final String token) {
        Matcher matcher = DIGITS_PATTERN.matcher(token);
        if (matcher.matches()) {
            return "all_digits";
        } else if (matcher.find()) {
            return "some_digits";
        } else {
            return "no_digits";
        }
    }

    private static String trailingZeros(final String token) {
        Matcher matcher = TRAILING_ZEROS_PATTERN.matcher(token);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    /**
     * Print a sequence to stdout. This is the equivalent of
     * {@code print(pycrfsuite.ItemSequence(features).items())} in the python
     * version.
     *
     * @param sequence the sequence to print.
     */
    private static void printItemSequence(final ItemSequence sequence) {
        for (int i = 0; i < sequence.size(); i++) {
            Item item = sequence.get(i);
            for (int j = 0; j < item.size(); j++) {
                Attribute attribute = item.get(j);
                System.out.println("'" + attribute.getAttr() + "': " + attribute.getValue() + ",");
            }
            System.out.println();
        }
    }

}
