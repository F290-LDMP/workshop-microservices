package br.com.fatecararas.api.core;

import br.com.fatecararas.api.exceptions.InvalidInputException;

public final class DomainValidation {
    private DomainValidation() { }
    public static void id(int value) {
        if (value < 1) throw new InvalidInputException("IDs must be positive: " + value);
    }
    public static void rate(int value) {
        if (value < 0 || value > 5) throw new InvalidInputException("Rate must be between 0 and 5");
    }
}
