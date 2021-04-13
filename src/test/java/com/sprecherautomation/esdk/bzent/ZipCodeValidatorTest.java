package com.sprecherautomation.esdk.bzent;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ZipCodeValidatorTest {

    ZipCodeValidator validator = new ZipCodeValidator();

    @Test
    public void canValidateValidGermanZipCode() {
        assertThat(validator.isGermanZipCode("71245"), is(true));
    }

    @Test
    public void canValidateInvalidGermanZipCode() {
        assertThat(validator.isGermanZipCode("0815"), is(false));
    }

}
