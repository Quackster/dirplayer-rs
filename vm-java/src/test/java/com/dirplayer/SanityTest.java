package com.dirplayer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic sanity tests.
 * Port of Rust vm-rust/tests/web.rs
 */
public class SanityTest {

    @Test
    void pass() {
        // Basic sanity check - equivalent to Rust test
        assertEquals(2, 1 + 1);
    }

    @Test
    void testDirPlayerInstantiation() {
        // Verify that core player classes can be instantiated
        com.dirplayer.player.DirPlayer player = new com.dirplayer.player.DirPlayer();
        assertNotNull(player);
        assertNotNull(player.movie);
        assertNotNull(player.globals);
        assertNotNull(player.allocator);
    }

    @Test
    void testColorRefCreation() {
        com.dirplayer.player.ColorRef color = new com.dirplayer.player.ColorRef(255, 128, 64);
        assertEquals(255, color.red);
        assertEquals(128, color.green);
        assertEquals(64, color.blue);
    }

    @Test
    void testCastMemberRefCreation() {
        com.dirplayer.player.CastMemberRef ref = new com.dirplayer.player.CastMemberRef(1, 5);
        assertEquals(1, ref.castLib);
        assertEquals(5, ref.castMember);
        assertTrue(ref.isValid());
    }
}
