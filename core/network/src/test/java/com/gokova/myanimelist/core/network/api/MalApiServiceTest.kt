package com.gokova.myanimelist.core.network.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MalApiServiceTest {
    @Test
    fun `default anime list fields requests list_status and not my_list_status`() {
        val fields = MalApiService.DEFAULT_ANIME_LIST_FIELDS
        assertTrue("Expected fields to contain list_status", fields.contains("list_status"))
        assertFalse("Fields must not contain my_list_status", fields.contains("my_list_status"))
    }
}
