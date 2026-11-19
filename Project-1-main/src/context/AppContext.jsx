import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { DEFAULT_CITY } from '../data/locations'
import {
  clearStoredUser,
  getDraft,
  getStoredCity,
  getStoredUser,
  saveDraft,
  setStoredCity,
  setStoredUser,
} from '../utils/storage'

const AppContext = createContext(null)

const emptyDraft = {
  movieId: null,
  theatreId: null,
  showId: null,
  date: null,
  seats: [],
}

export function AppProvider({ children }) {
  const [city, setCityState] = useState(getStoredCity() || DEFAULT_CITY)
  const [user, setUser] = useState(getStoredUser())
  const [searchQuery, setSearchQuery] = useState('')
  const [draft, setDraftState] = useState(getDraft() || emptyDraft)
  const [loginModalOpen, setLoginModalOpen] = useState(false)

  // Catalogue comes from MySQL through the Java API.
  const [movies, setMovies] = useState([])
  const [loadingMovies, setLoadingMovies] = useState(true)
  const [apiError, setApiError] = useState('')

  useEffect(() => {
    let active = true
    api
      .movies()
      .then((list) => {
        if (!active) return
        setMovies(list)
        setApiError('')
      })
      .catch((error) => {
        if (!active) return
        setApiError(error.message)
      })
      .finally(() => {
        if (active) setLoadingMovies(false)
      })
    return () => {
      active = false
    }
  }, [])

  const setCity = (next) => {
    setCityState(next)
    setStoredCity(next)
  }

  const login = (profile) => {
    setUser(profile)
    setStoredUser(profile)
  }

  const logout = () => {
    setUser(null)
    clearStoredUser()
  }

  const updateDraft = (patch) => {
    setDraftState((prev) => {
      const next = { ...prev, ...patch }
      saveDraft(next)
      return next
    })
  }

  const resetDraft = () => {
    setDraftState(emptyDraft)
    saveDraft(emptyDraft)
  }

  const value = useMemo(
    () => ({
      city,
      setCity,
      user,
      login,
      logout,
      searchQuery,
      setSearchQuery,
      draft,
      updateDraft,
      resetDraft,
      loginModalOpen,
      setLoginModalOpen,
      movies,
      loadingMovies,
      apiError,
    }),
    [city, user, searchQuery, draft, loginModalOpen, movies, loadingMovies, apiError],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp() {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('useApp must be used within AppProvider')
  return ctx
}
