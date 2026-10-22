package com.hdlee73.everylaw.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test fun comparesNumerically() {
        assertTrue(UpdateManager.isNewer("1.10.0", "1.9.3"))
        assertTrue(UpdateManager.isNewer("2.0", "1.99.99"))
        assertFalse(UpdateManager.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateManager.isNewer("1.0.0", "1.0.1-debug"))
        assertTrue(UpdateManager.isNewer("1.0.1", "1.0.0-debug"))
    }
}
