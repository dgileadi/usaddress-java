package io.github.dgileadi.usaddress;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A parsed address.
 */
public class Address {

    public enum Type {
        STREET_ADDRESS,
        INTERSECTION,
        PO_BOX,
        AMBIGUOUS
    }

    public enum FieldType {
        ADDRESS_NUMBER("AddressNumber"),
        STREET_NAME_PRE_DIRECTIONAL("StreetNamePreDirectional"),
        STREET_NAME("StreetName"),
        SECOND_STREET_NAME("SecondStreetName"),
        STREET_NAME_POST_TYPE("StreetNamePostType"),
        OCCUPANCY_IDENTIFIER("OccupancyIdentifier"),
        OCCUPANCY_TYPE("OccupancyType"),
        STREET_NAME_PRE_TYPE("StreetNamePreType"),
        PLACE_NAME("PlaceName"),
        ZIP_CODE("ZipCode"),
        STATE_NAME("StateName"),
        LANDMARK_NAME("LandmarkName"),
        USPS_BOX_TYPE("USPSBoxType"),
        USPS_BOX_ID("USPSBoxID"),
        STREET_NAME_POST_DIRECTIONAL("StreetNamePostDirectional"),
        ADDRESS_NUMBER_SUFFIX("AddressNumberSuffix"),
        USPS_BOX_GROUP_ID("USPSBoxGroupID"),
        USPS_BOX_GROUP_TYPE("USPSBoxGroupType"),
        SUBADDRESS_IDENTIFIER("SubaddressIdentifier"),
        SUBADDRESS_TYPE("SubaddressType"),
        RECIPIENT("Recipient"),
        STREET_NAME_PRE_MODIFIER("StreetNamePreModifier"),
        BUILDING_NAME("BuildingName"),
        ADDRESS_NUMBER_PREFIX("AddressNumberPrefix"),
        INTERSECTION_SEPARATOR("IntersectionSeparator"),
        CORNER_OF("CornerOf"),
        NOT_ADDRESS("NotAddress"),
        STREET_NAME_POST_MODIFIER("StreetNamePostModifier"),
        COUNTRY_NAME("CountryName"),
        ZIP_PLUS4("ZipPlus4");

        private String label;

        public String getLabel() {
            return label;
        }

        private FieldType(String label) {
            this.label = label;
        }

        private static FieldType forLabel(String label) {
            for (FieldType fieldType : FieldType.values()) {
                if (fieldType.label.equals(label)) {
                    return fieldType;
                }
            }
            throw new IllegalArgumentException(label);
        }
    }

    public static class Field {
        private static final Pattern CLEAN_VALUE_PATTERN = Pattern.compile("[\\s,;]+$|^[\\s,;]+");

        private String value;
        private FieldType type;

        Field(final String value, final String type) {
            this(value, FieldType.forLabel(type));
        }

        Field(final String value, final FieldType type) {
            this.value = value;
            this.type = type;
        }

        /**
         * @return the field value, such as {@code Nowhereville}
         */
        public String getValue() {
            return value;
        }

        /**
         * @return the field type, such as {@code PlaceName}
         */
        public FieldType getType() {
            return type;
        }

        void appendValue(String suffix) {
            value = value + " " + suffix;
        }

        void cleanValue() {
            value = CLEAN_VALUE_PATTERN.matcher(value).replaceAll("");
        }
    }

    private Type type;
    private List<Field> fields;

    protected Address(Type type, List<Field> fields) {
        this.type = type;
        this.fields = fields;
    }

    /**
     * @return the detected type of the address
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the parsed fields of the address, in parsed order. Each key is the
     *         detected filed type and each corresponding value is the parsed field
     *         value.
     */
    public List<Field> getFields() {
        return fields;
    }

}
