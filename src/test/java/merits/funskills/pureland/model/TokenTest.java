package merits.funskills.pureland.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenTest {

    @Test
    public void testTokenValid() {
        assertTrue(Token.isValidToken("4b19fd6f-887e-4580-98a3-827af31bff06,Ten,0"));
        assertTrue(Token.isValidToken("4b19fd6f-887e-4580-98a3-827af31bff06,Ten,0,1"));
        assertFalse(Token.isValidToken(null));
        assertFalse(Token.isValidToken("123456"));
    }
}
