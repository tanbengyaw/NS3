package my.gov.perkeso.assist.registration.constant;

public enum CheckDigitTableEnum {
    B(1.0D),
    F(2.0D),
    K(3.0D),
    M(4.0D),
    P(5.0D),
    V(6.0D),
    W(7.0D),
    X(8.0D),
    Y(9.0D),
    Z(10.0D),
    A(11.0D);

    private final double value;

    CheckDigitTableEnum(final double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public static CheckDigitTableEnum fromValue(final double value) {
        for (final CheckDigitTableEnum digit : values()) {
            if (Double.compare(digit.getValue(), value) == 0) {
                return digit;
            }
        }
        throw new IllegalArgumentException("Enum for Check Digit not found. Value:" + value);
    }
}
