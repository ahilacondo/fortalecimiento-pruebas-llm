package org.apache.commons.codec.binary;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class Base64LLMTest {

    @Test
    public void encodeEmptyStringReturnsEmptyString() {
        assertEquals("", Base64.encodeBase64String(new byte[0]));
    }

    @Test
    public void encodeFoobarMatchesRfc4648Vector() {
        byte[] input = "foobar".getBytes(StandardCharsets.US_ASCII);
        assertEquals("Zm9vYmFy", Base64.encodeBase64String(input));
    }

    @Test
    public void decodeFoobarMatchesRfc4648Vector() {
        byte[] expected = "foobar".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(expected, Base64.decodeBase64("Zm9vYmFy"));
    }

    @Test
    public void decodeEmptyStringReturnsEmptyArray() {
        assertArrayEquals(new byte[0], Base64.decodeBase64(""));
    }
}
