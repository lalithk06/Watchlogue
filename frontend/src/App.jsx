import { useCallback, useEffect, useState } from 'react'
import './App.css'

const API_BASE_URL = 'http://localhost:8080/api'

const WATCH_STATUSES = [
  { value: 'PLANNED', label: 'Plan to watch' },
  { value: 'WATCHING', label: 'Watching' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'ON_HOLD', label: 'On hold' },
  { value: 'DROPPED', label: 'Dropped' },
]

const EMPTY_FILTERS = {
  status: 'ALL',
  mediaType: 'ALL',
  minTmdbRating: '7',
  minPersonalRating: '0',
  yearFrom: '2000',
  yearTo: '2026',
}

function App() {
  const [library, setLibrary] = useState([])
  const [allLibrary, setAllLibrary] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [addingId, setAddingId] = useState(null)
  const [addMessage, setAddMessage] = useState('')
  const [actionError, setActionError] = useState('')
  const [updatingStatusId, setUpdatingStatusId] = useState(null)
  const [savingEntry, setSavingEntry] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [editRating, setEditRating] = useState('')
  const [editNotes, setEditNotes] = useState('')
  const [activeView, setActiveView] = useState('library')

  const [filters, setFilters] = useState({ ...EMPTY_FILTERS })

  const updateFilter = (event) => {
    const { name, value } = event.target
    setFilters((previous) => ({ ...previous, [name]: value }))
  }

  const fetchLibrary = useCallback(async () => {
    setLoading(true)
    setError('')

    const params = new URLSearchParams()

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '') params.set(key, value)
    })

    try {
      const response = await fetch(
        `${API_BASE_URL}/library?${params.toString()}`,
      )

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`)
      }

      const data = await response.json()
      setLibrary(data)
    } catch {
      setError(
        'Could not load your library. Make sure the Spring Boot backend is running on port 8080.',
      )
    } finally {
      setLoading(false)
    }
  }, [filters])

  const fetchAllLibrary = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/library`)

      if (!response.ok) {
        throw new Error('Could not load library statistics.')
      }

      const data = await response.json()
      setAllLibrary(data)
    } catch {
      // Keep the main library usable if the separate statistics request fails.
    }
  }, [])

  const refreshLibraryData = useCallback(async () => {
    await Promise.all([fetchLibrary(), fetchAllLibrary()])
  }, [fetchLibrary, fetchAllLibrary])

  useEffect(() => {
    refreshLibraryData()
  }, [refreshLibraryData])

  useEffect(() => {
    const query = search.trim()

    if (activeView !== 'discover' || query.length < 2) {
      setSearchResults([])
      setSearchLoading(false)
      setSearchError('')
      return
    }

    let cancelled = false

    const timer = setTimeout(async () => {
      setSearchLoading(true)
      setSearchError('')

      try {
        const response = await fetch(
          `${API_BASE_URL}/search?query=${encodeURIComponent(query)}`,
        )

        if (!response.ok) {
          throw new Error(`Search failed with status ${response.status}`)
        }

        const data = await response.json()

        if (!cancelled) setSearchResults(data)
      } catch {
        if (!cancelled) {
          setSearchResults([])
          setSearchError(
            'Could not search TMDB. Check your backend connection and try again.',
          )
        }
      } finally {
        if (!cancelled) setSearchLoading(false)
      }
    }, 350)

    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [search, activeView])

  const addToLibrary = async (item) => {
    setAddingId(item.tmdbId)
    setAddMessage('')
    setActionError('')

    try {
      const response = await fetch(`${API_BASE_URL}/library`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tmdbId: item.tmdbId,
          mediaType: item.mediaType,
        }),
      })

      if (!response.ok) {
        throw new Error(`Could not add title (HTTP ${response.status}).`)
      }

      setAddMessage(`"${item.title}" added to your library.`)
      await refreshLibraryData()
    } catch (err) {
      setActionError(err.message || 'Could not add this title.')
    } finally {
      setAddingId(null)
    }
  }

  const updateWatchStatus = async (item, newStatus) => {
    const previousStatus = item.status
    setUpdatingStatusId(item.id)
    setActionError('')

    // Update the visible cards immediately; restore them if the request fails.
    const updateStatusIn = (items) =>
      items.map((entry) =>
        entry.id === item.id ? { ...entry, status: newStatus } : entry,
      )

    setLibrary(updateStatusIn)
    setAllLibrary(updateStatusIn)

    try {
      const response = await fetch(
        `${API_BASE_URL}/library/${item.id}/status`,
        {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: newStatus }),
        },
      )

      if (!response.ok) {
        throw new Error(`Could not update status (HTTP ${response.status}).`)
      }

      const updatedEntry = await response.json()

      const updateEntryIn = (items) =>
        items.map((entry) =>
          entry.id === item.id ? { ...entry, ...updatedEntry } : entry,
        )

      setLibrary(updateEntryIn)
      setAllLibrary(updateEntryIn)
    } catch (err) {
      const restoreStatus = (items) =>
        items.map((entry) =>
          entry.id === item.id
            ? { ...entry, status: previousStatus }
            : entry,
        )

      setLibrary(restoreStatus)
      setAllLibrary(restoreStatus)
      setActionError(err.message || 'Could not update watch status.')
    } finally {
      setUpdatingStatusId(null)
    }
  }

  const openEditDialog = (item) => {
    setEditingItem(item)
    setEditRating(item.rating == null ? '' : String(item.rating))
    setEditNotes(item.notes ?? '')
    setActionError('')
  }

  const saveLibraryEntry = async (event) => {
    event.preventDefault()

    if (!editingItem) return

    if (editRating !== '') {
      const rating = Number(editRating)

      if (!Number.isInteger(rating) || rating < 1 || rating > 10) {
        setActionError('Your rating must be a whole number from 1 to 10.')
        return
      }
    }

    setSavingEntry(true)
    setActionError('')

    try {
      const response = await fetch(
        `${API_BASE_URL}/library/${editingItem.id}`,
        {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            rating: editRating === '' ? null : Number(editRating),
            notes: editNotes,
          }),
        },
      )

      if (!response.ok) {
        throw new Error(`Could not save changes (HTTP ${response.status}).`)
      }

      const updatedEntry = await response.json()

      const updateEntryIn = (items) =>
        items.map((entry) =>
          entry.id === editingItem.id
            ? { ...entry, ...updatedEntry }
            : entry,
        )

      setLibrary(updateEntryIn)
      setAllLibrary(updateEntryIn)
      setEditingItem(null)
      setAddMessage(`Changes saved for "${editingItem.title}".`)
    } catch (err) {
      setActionError(err.message || 'Could not save your changes.')
    } finally {
      setSavingEntry(false)
    }
  }

  const visibleLibrary = library.filter((item) =>
    item.title.toLowerCase().includes(search.toLowerCase()),
  )

  const libraryIds = new Set(allLibrary.map((item) => item.tmdbId))
  const movieCount = allLibrary.filter(
    (item) => item.mediaType === 'MOVIE',
  ).length
  const tvCount = allLibrary.filter((item) => item.mediaType === 'TV').length
  const completedCount = allLibrary.filter(
    (item) => item.status === 'COMPLETED',
  ).length
  const ratedItems = allLibrary.filter((item) => item.rating != null)
  const averageRating =
    ratedItems.length > 0
      ? (
          ratedItems.reduce((sum, item) => sum + item.rating, 0) /
          ratedItems.length
        ).toFixed(1)
      : '—'

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          className="brand"
          href="#library"
          onClick={() => setActiveView('library')}
        >
          <span className="brand-mark">W</span>
          <span>watchlogue</span>
        </a>

        <p className="nav-label">WORKSPACE</p>
        <nav className="navigation">
          <a
            className={`nav-item ${activeView === 'library' ? 'active' : ''}`}
            href="#library"
            onClick={() => {
              setActiveView('library')
              setAddMessage('')
            }}
          >
            <span>▦</span> My Library
          </a>
          <a
            className={`nav-item ${activeView === 'discover' ? 'active' : ''}`}
            href="#discover"
            onClick={() => {
              setActiveView('discover')
              setAddMessage('')
            }}
          >
            <span>⌕</span> Discover
          </a>
        </nav>

        <div className="sidebar-bottom">
          <div className="profile-avatar">W</div>
          <div>
            <p className="profile-name">My Watchlogue</p>
            <p className="profile-caption">Personal collection</p>
          </div>
        </div>
      </aside>

      <main
        className="main-content"
        id={activeView === 'library' ? 'library' : 'discover'}
      >
        <header className="topbar">
          <div className="breadcrumb">
            Workspace /{' '}
            <strong>
              {activeView === 'library' ? 'My Library' : 'Discover'}
            </strong>
          </div>
          <div className="connection-status">
            <span className="status-dot" />
            Your collection
          </div>
        </header>

        {activeView === 'library' && (
          <section className="stats-grid">
            <article className="stat-card">
              <span className="stat-label">TOTAL TITLES</span>
              <strong>{allLibrary.length}</strong>
              <span className="stat-caption">In your collection</span>
            </article>
            <article className="stat-card">
              <span className="stat-label">MOVIES</span>
              <strong>{movieCount}</strong>
              <span className="stat-caption">Films collected</span>
            </article>
            <article className="stat-card">
              <span className="stat-label">TV SERIES</span>
              <strong>{tvCount}</strong>
              <span className="stat-caption">Series collected</span>
            </article>
            <article className="stat-card">
              <span className="stat-label">COMPLETED</span>
              <strong>{completedCount}</strong>
              <span className="stat-caption">Titles completed</span>
            </article>
            <article className="stat-card">
              <span className="stat-label">YOUR AVG. RATING</span>
              <strong>{averageRating}</strong>
              <span className="stat-caption">
                {ratedItems.length} rated {ratedItems.length === 1 ? 'title' : 'titles'}
              </span>
            </article>
          </section>
        )}

        <section className="page-heading">
          <div>
            <p className="eyebrow">
              {activeView === 'library'
                ? 'YOUR PERSONAL CINEMA'
                : 'FIND YOUR NEXT STORY'}
            </p>
            <h1>
              {activeView === 'library' ? 'My Library' : 'Discover'}
              <span className="heading-period">.</span>
            </h1>
            <p className="page-description">
              {activeView === 'library'
                ? 'Every story you watch, all in one place.'
                : 'Search movies and TV series to add to your collection.'}
            </p>
          </div>

          {activeView === 'library' && (
            <div className="library-count">
              <span>{visibleLibrary.length}</span>
              <small>
                {visibleLibrary.length === 1 ? 'TITLE' : 'TITLES'}
              </small>
            </div>
          )}
        </section>

        <section className="toolbar">
          <label className="search-box">
            <span>⌕</span>
            <input
              type="search"
              placeholder={
                activeView === 'library'
                  ? 'Search your collection...'
                  : 'Search movies and TV shows...'
              }
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setAddMessage('')
              }}
            />
            <kbd>⌘ K</kbd>
          </label>

          {activeView === 'library' && (
            <button className="refresh-button" onClick={refreshLibraryData}>
              ↻ <span>Refresh</span>
            </button>
          )}
        </section>

        {actionError && (
          <div className="action-alert" role="alert">
            <span>{actionError}</span>
            <button onClick={() => setActionError('')} aria-label="Dismiss error">
              ×
            </button>
          </div>
        )}

        {addMessage && (
          <div className="action-success" role="status">
            {addMessage}
          </div>
        )}

        {activeView === 'discover' ? (
          <section className="collection-section">
            <div className="collection-heading">
              <div>
                <h2>Search results</h2>
                <p>Results powered by TMDB</p>
              </div>
              <span className="result-count">
                {searchResults.length} results
              </span>
            </div>

            {!search.trim() || search.trim().length < 2 ? (
              <div className="message-state">
                <span className="empty-icon">⌕</span>
                <h3>Find something to watch</h3>
                <p>Enter at least two characters to search movies and TV shows.</p>
              </div>
            ) : searchLoading ? (
              <div className="message-state">Searching TMDB...</div>
            ) : searchError ? (
              <div className="message-state error-state">
                <p>{searchError}</p>
              </div>
            ) : searchResults.length === 0 ? (
              <div className="message-state">
                <span className="empty-icon">▧</span>
                <h3>No results found</h3>
                <p>Try another title or spelling.</p>
              </div>
            ) : (
              <div className="media-grid">
                {searchResults.map((item) => {
                  const alreadyAdded = libraryIds.has(item.tmdbId)
                  const isAdding = addingId === item.tmdbId

                  return (
                    <article
                      className="media-card"
                      key={`${item.mediaType}-${item.tmdbId}`}
                    >
                      <div className="poster-wrapper">
                        {item.posterUrl ? (
                          <img
                            className="poster"
                            src={item.posterUrl}
                            alt={`${item.title} poster`}
                            loading="lazy"
                          />
                        ) : (
                          <div className="poster-placeholder">No poster</div>
                        )}
                        <span className="format-badge">
                          {item.mediaType === 'TV' ? 'TV SERIES' : 'MOVIE'}
                        </span>
                        {item.tmdbRating != null && (
                          <span className="tmdb-badge">
                            ★ {item.tmdbRating.toFixed(1)}
                          </span>
                        )}
                      </div>

                      <div className="media-details">
                        <div className="media-title-row">
                          <h3 title={item.title}>{item.title}</h3>
                        </div>
                        <p className="media-meta">
                          {item.year ?? 'Year unknown'}
                          <span>·</span>
                          {item.mediaType === 'TV' ? 'TV series' : 'Movie'}
                        </p>
                        <button
                          className="apply-button"
                          disabled={isAdding || alreadyAdded}
                          onClick={() => addToLibrary(item)}
                        >
                          {isAdding
                            ? 'Adding...'
                            : alreadyAdded
                              ? '✓ In library'
                              : '+ Add to library'}
                        </button>
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        ) : (
          <>
            <section className="filter-panel">
              <div className="filter-heading">
                <div>
                  <span className="filter-icon">☷</span>
                  <strong>Filters</strong>
                </div>
                <button
                  className="reset-button"
                  onClick={() => setFilters({
                    ...EMPTY_FILTERS,
                    minTmdbRating: '',
                    minPersonalRating: '0',
                    yearFrom: '',
                    yearTo: '',
                  })}
                >
                  Reset filters
                </button>
              </div>

              <div className="filter-grid">
                <label className="filter-field">
                  <span>WATCH STATUS</span>
                  <select name="status" value={filters.status} onChange={updateFilter}>
                    <option value="ALL">All statuses</option>
                    {WATCH_STATUSES.map((status) => (
                      <option key={status.value} value={status.value}>
                        {status.label}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="filter-field">
                  <span>FORMAT</span>
                  <select name="mediaType" value={filters.mediaType} onChange={updateFilter}>
                    <option value="ALL">Movies & TV</option>
                    <option value="MOVIE">Movies</option>
                    <option value="TV">TV series</option>
                  </select>
                </label>

                <label className="filter-field">
                  <span>MIN. TMDB RATING</span>
                  <select name="minTmdbRating" value={filters.minTmdbRating} onChange={updateFilter}>
                    <option value="">Any rating</option>
                    {[5, 6, 7, 8, 9].map((rating) => (
                      <option key={rating} value={rating}>{rating}.0+</option>
                    ))}
                  </select>
                </label>

                <label className="filter-field">
                  <span>MIN. YOUR RATING</span>
                  <select name="minPersonalRating" value={filters.minPersonalRating} onChange={updateFilter}>
                    <option value="0">Any rating</option>
                    {[5, 6, 7, 8, 9].map((rating) => (
                      <option key={rating} value={rating}>{rating}/10+</option>
                    ))}
                  </select>
                </label>

                <label className="filter-field">
                  <span>YEAR FROM</span>
                  <input type="number" name="yearFrom" min="1888" max="2100" placeholder="Any year" value={filters.yearFrom} onChange={updateFilter} />
                </label>

                <label className="filter-field">
                  <span>YEAR TO</span>
                  <input type="number" name="yearTo" min="1888" max="2100" placeholder="Any year" value={filters.yearTo} onChange={updateFilter} />
                </label>
              </div>

              <button className="apply-button" onClick={fetchLibrary}>
                Apply filters <span>→</span>
              </button>
            </section>

            <section className="collection-section">
              <div className="collection-heading">
                <div>
                  <h2>Your collection</h2>
                  <p>Manage your watch status, ratings, and notes</p>
                </div>
                <span className="result-count">{visibleLibrary.length} results</span>
              </div>

              {loading ? (
                <div className="message-state">Loading your collection...</div>
              ) : error ? (
                <div className="message-state error-state">
                  <p>{error}</p>
                  <button className="apply-button" onClick={refreshLibraryData}>Try again</button>
                </div>
              ) : visibleLibrary.length === 0 ? (
                <div className="message-state">
                  <span className="empty-icon">▧</span>
                  <h3>No titles found</h3>
                  <p>Try changing your filters or search terms.</p>
                </div>
              ) : (
                <div className="media-grid">
                  {visibleLibrary.map((item) => (
                    <article className="media-card" key={item.id}>
                      <div className="poster-wrapper">
                        {item.posterUrl ? (
                          <img className="poster" src={item.posterUrl} alt={`${item.title} poster`} loading="lazy" />
                        ) : (
                          <div className="poster-placeholder">No poster</div>
                        )}
                        <span className="format-badge">
                          {item.mediaType === 'TV' ? 'TV SERIES' : 'MOVIE'}
                        </span>
                        {item.tmdbRating != null && (
                          <span className="tmdb-badge">★ {item.tmdbRating.toFixed(1)}</span>
                        )}
                      </div>

                      <div className="media-details">
                        <div className="media-title-row">
                          <h3 title={item.title}>{item.title}</h3>
                          {item.rating != null && (
                            <span className="personal-rating">♥ {item.rating}</span>
                          )}
                        </div>

                        <p className="media-meta">
                          {item.year ?? 'Year unknown'}
                          <span>·</span>
                          {item.status.replace('_', ' ')}
                        </p>

                        <label className="card-control">
                          <span>WATCH STATUS</span>
                          <select
                            value={item.status}
                            disabled={updatingStatusId === item.id}
                            onChange={(event) => updateWatchStatus(item, event.target.value)}
                            aria-label={`Watch status for ${item.title}`}
                          >
                            {WATCH_STATUSES.map((status) => (
                              <option key={status.value} value={status.value}>
                                {status.label}
                              </option>
                            ))}
                          </select>
                        </label>

                        {item.notes && <p className="media-notes">{item.notes}</p>}

                        <button className="edit-entry-button" onClick={() => openEditDialog(item)}>
                          ✎ Edit rating & notes
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}

        <footer className="footer">
          <span>WATCHLOGUE</span>
          <span>Made for the stories that stay with you.</span>
        </footer>
      </main>

      {editingItem && (
        <div className="modal-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !savingEntry) setEditingItem(null)
        }}>
          <section className="edit-modal" role="dialog" aria-modal="true" aria-labelledby="edit-title">
            <div className="modal-heading">
              <div>
                <p className="eyebrow">YOUR PERSONAL REVIEW</p>
                <h2 id="edit-title">Edit title</h2>
                <p className="modal-subtitle">{editingItem.title}</p>
              </div>
              <button className="modal-close" onClick={() => setEditingItem(null)} disabled={savingEntry} aria-label="Close dialog">×</button>
            </div>

            <form onSubmit={saveLibraryEntry}>
              <label className="modal-field">
                <span>YOUR RATING (1–10)</span>
                <select value={editRating} onChange={(event) => setEditRating(event.target.value)}>
                  <option value="">Not rated</option>
                  {[10, 9, 8, 7, 6, 5, 4, 3, 2, 1].map((rating) => (
                    <option key={rating} value={rating}>{rating}/10</option>
                  ))}
                </select>
              </label>

              <label className="modal-field">
                <span>YOUR NOTES</span>
                <textarea
                  value={editNotes}
                  onChange={(event) => setEditNotes(event.target.value)}
                  maxLength={2000}
                  rows={5}
                  placeholder="What did you think of this title?"
                />
                <small>{editNotes.length}/2000 characters</small>
              </label>

              {actionError && <p className="modal-error" role="alert">{actionError}</p>}

              <div className="modal-actions">
                <button type="button" className="reset-button" onClick={() => setEditingItem(null)} disabled={savingEntry}>
                  Cancel
                </button>
                <button type="submit" className="apply-button" disabled={savingEntry}>
                  {savingEntry ? 'Saving...' : 'Save changes'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  )
}

export default App
