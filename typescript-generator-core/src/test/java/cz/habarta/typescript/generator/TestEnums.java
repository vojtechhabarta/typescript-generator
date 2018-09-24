package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class TestEnums {
    public enum StandardEnum {
        A, B, C
    }

    public enum GeneralMethodValuedEnum {
        A, B, C;

        @JsonValue
        public String getValue() {
            return "_" + name();
        }
    }

    public enum NumberFieldValuedEnum {
        A(1), B(2), C(3);

        @JsonValue
        private int value;

        NumberFieldValuedEnum(int value) {
            this.value = value;
        }
    }
    
    public enum NumberMethodValuedEnum {
        A(1), B(2), C(3);

        private int value;

        NumberMethodValuedEnum(int value) {
            this.value = value;
        }

        @JsonValue
        public int getValue() {
            return value;
        }
    }

    public enum ToStringValuedEnum {
        A, B, C;

        @JsonValue
        @Override
        public String toString() {
            return "_" + name();
        }
    }

    public enum StringPropertyValuedEnum {
        A, B, C;

        @JsonValue
        private final String value = "_" + name();
    }

    public enum NumberPropertyValuedEnum {
        A, B, C;

        @JsonValue
        private final Integer value = ordinal();
    }

    public enum JsonPropertyValuedEnum {
        @JsonProperty("_A") A,
        @JsonProperty("_B") B,
        @JsonProperty("_C") C
    }
}
