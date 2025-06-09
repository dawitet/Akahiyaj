package com.dawitf.akahidegn.viewmodel

import androidx.paging.PagingData
import com.dawitf.akahidegn.core.result.Result
import com.dawitf.akahidegn.domain.model.*
import com.dawitf.akahidegn.domain.repository.EnhancedGroupRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class EnhancedSearchViewModelTest {

    private lateinit var viewModel: EnhancedSearchViewModel
    private val mockRepository = mockk<EnhancedGroupRepository>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EnhancedSearchViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initial state should be correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(SearchFilters(), state.currentFilters)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.recentSearches.isEmpty())
        assertTrue(state.suggestions.isEmpty())
    }

    @Test
    fun `search should update query and trigger search`() = runTest {
        // Given
        val query = "Bole"
        val mockGroups = listOf(createMockGroup("1", "Bole"))
        val mockPagingData = PagingData.from(mockGroups)
        
        every { mockRepository.searchGroups(any(), any()) } returns flowOf(mockPagingData)
        coEvery { mockRepository.saveRecentSearch(any()) } returns Result.Success(Unit)

        // When
        viewModel.onSearchQueryChanged(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        verify { mockRepository.searchGroups(query, any()) }
        coVerify { mockRepository.saveRecentSearch(query) }
    }

    @Test
    fun `filter application should update filters and trigger search`() = runTest {
        // Given
        val filters = SearchFilters(
            maxDistance = 5.0,
            priceRange = PriceRange(0.0, 100.0),
            timeRange = TimeRange(
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis() + 3600000
            )
        )
        val mockPagingData = PagingData.from(emptyList<Group>())
        
        every { mockRepository.searchGroups(any(), any()) } returns flowOf(mockPagingData)

        // When
        viewModel.applyFilters(filters)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(filters, state.currentFilters)
        verify { mockRepository.searchGroups(any(), filters) }
    }

    @Test
    fun `clear filters should reset filters and trigger search`() = runTest {
        // Given
        val initialFilters = SearchFilters(maxDistance = 5.0)
        val mockPagingData = PagingData.from(emptyList<Group>())
        
        every { mockRepository.searchGroups(any(), any()) } returns flowOf(mockPagingData)
        
        viewModel.applyFilters(initialFilters)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(SearchFilters(), state.currentFilters)
        verify(atLeast = 2) { mockRepository.searchGroups(any(), any()) }
    }

    @Test
    fun `get suggestions should return autocomplete suggestions`() = runTest {
        // Given
        val query = "Bo"
        val mockSuggestions = listOf("Bole", "Bole Medhane Alem", "Bole Atlas")
        
        coEvery { mockRepository.getAutocompleteSuggestions(query) } returns Result.Success(mockSuggestions)

        // When
        viewModel.getSuggestions(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mockSuggestions, state.suggestions)
        coVerify { mockRepository.getAutocompleteSuggestions(query) }
    }

    @Test
    fun `load recent searches should update recent searches list`() = runTest {
        // Given
        val mockRecentSearches = listOf("Bole", "Megenagna", "Piassa")
        
        coEvery { mockRepository.getRecentSearches() } returns Result.Success(mockRecentSearches)

        // When
        viewModel.loadRecentSearches()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mockRecentSearches, state.recentSearches)
        coVerify { mockRepository.getRecentSearches() }
    }

    @Test
    fun `error handling should update error state`() = runTest {
        // Given
        val errorMessage = "Network error"
        
        coEvery { mockRepository.getRecentSearches() } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.loadRecentSearches()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `clear error should reset error state`() = runTest {
        // Given
        coEvery { mockRepository.getRecentSearches() } returns Result.Error(Exception("Error"))
        viewModel.loadRecentSearches()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    private fun createMockGroup(id: String, destination: String): Group {
        return Group(
            id = id,
            destination = destination,
            driverId = "driver1",
            driverName = "Test Driver",
            currentMembers = 1,
            maxMembers = 4,
            departureTime = System.currentTimeMillis(),
            pricePerSeat = 50.0,
            pickupLocation = Location(9.005401, 38.763611),
            dropoffLocation = Location(8.980603, 38.757761),
            status = "active",
            createdAt = System.currentTimeMillis(),
            isActive = true,
            description = "Test group",
            vehicleInfo = VehicleInfo("Toyota", "Corolla", "Blue", "AA-12345"),
            contactInfo = ContactInfo("0911123456", null, null)
        )
    }
}
