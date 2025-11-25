package com.helpdesk.app

import com.helpdesk.app.util.Result
import org.junit.Test
import org.junit.Assert.*

class ResultTest {

    @Test
    fun `Success result returns correct data`() {
        val result: Result<String> = Result.Success("test data")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals("test data", result.getOrNull())
        assertEquals("test data", result.getOrThrow())
    }

    @Test
    fun `Error result returns exception`() {
        val exception = Exception("test error")
        val result: Result<String> = Result.Error(exception)
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
        assertNull(result.getOrNull())
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `Loading result has correct flags`() {
        val result: Result<String> = Result.Loading
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
        assertNull(result.getOrNull())
    }

    @Test
    fun `map transforms Success data`() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Success)
        assertEquals(10, (mapped as Result.Success).data)
    }

    @Test
    fun `map preserves Error`() {
        val exception = Exception("test")
        val result: Result<Int> = Result.Error(exception)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Error)
        assertEquals(exception, (mapped as Result.Error).exception)
    }

    @Test
    fun `fold calls correct branch for Success`() {
        val result: Result<Int> = Result.Success(10)
        val value = result.fold(
            onSuccess = { it * 2 },
            onError = { -1 },
            onLoading = { 0 }
        )
        assertEquals(20, value)
    }

    @Test
    fun `fold calls correct branch for Error`() {
        val result: Result<Int> = Result.Error(Exception("test"))
        val value = result.fold(
            onSuccess = { it * 2 },
            onError = { -1 },
            onLoading = { 0 }
        )
        assertEquals(-1, value)
    }

    @Test
    fun `fold calls correct branch for Loading`() {
        val result: Result<Int> = Result.Loading
        val value = result.fold(
            onSuccess = { it * 2 },
            onError = { -1 },
            onLoading = { 0 }
        )
        assertEquals(0, value)
    }

    @Test
    fun `onSuccess executes action for Success`() {
        var executed = false
        val result: Result<Int> = Result.Success(5)
        result.onSuccess { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `onSuccess does not execute for Error`() {
        var executed = false
        val result: Result<Int> = Result.Error(Exception("test"))
        result.onSuccess { executed = true }
        assertFalse(executed)
    }

    @Test
    fun `onError executes action for Error`() {
        var executed = false
        val result: Result<Int> = Result.Error(Exception("test"))
        result.onError { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `onError does not execute for Success`() {
        var executed = false
        val result: Result<Int> = Result.Success(5)
        result.onError { executed = true }
        assertFalse(executed)
    }

    @Test(expected = Exception::class)
    fun `getOrThrow throws for Error`() {
        val result: Result<Int> = Result.Error(Exception("test"))
        result.getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws for Loading`() {
        val result: Result<Int> = Result.Loading
        result.getOrThrow()
    }
}
