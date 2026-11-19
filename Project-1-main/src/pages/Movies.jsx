import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import EmptyState, { Loader } from '../components/EmptyState'
import MovieGrid from '../components/MovieGrid'
import { useApp } from '../context/AppContext'

export default function Movies() {
  const [params] = useSearchParams()
  const { searchQuery, setSearchQuery, movies, loadingMovies } = useApp()
  const [genre, setGenre] = useState('all')
  const [language, setLanguage] = useState('all')
  const [rating, setRating] = useState('all')
  const [sort, setSort] = useState('popularity')

  useEffect(() => {
    const q = params.get('q')
    if (q) setSearchQuery(q)
  }, [params, setSearchQuery])

  const genres = useMemo(
    () => [...new Set(movies.flatMap((m) => m.genres))].sort(),
    [movies],
  )
  const languages = useMemo(() => [...new Set(movies.map((m) => m.language))].sort(), [movies])

  const list = useMemo(() => {
    const next = movies.filter((m) => {
      const q = searchQuery.trim().toLowerCase()
      const matchesQuery =
        !q ||
        m.title.toLowerCase().includes(q) ||
        m.genres.join(' ').toLowerCase().includes(q) ||
        m.language.toLowerCase().includes(q)
      const matchesGenre = genre === 'all' || m.genres.includes(genre)
      const matchesLang = language === 'all' || m.language === language
      const matchesRating =
        rating === 'all' ||
        (rating === '8' && m.rating >= 8) ||
        (rating === '7' && m.rating >= 7)
      return matchesQuery && matchesGenre && matchesLang && matchesRating
    })
    return [...next].sort((a, b) => {
      if (sort === 'rating') return b.rating - a.rating
      return b.popularity - a.popularity
    })
  }, [movies, searchQuery, genre, language, rating, sort])

  return (
    <section className="section container">
      <h1 className="page-title">All titles</h1>
      <p className="muted">Filter the catalogue. Coming soon titles can be browsed but not booked.</p>
      <div className="filters">
        <input
          className="field"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search"
        />
        <select className="select" value={genre} onChange={(e) => setGenre(e.target.value)}>
          <option value="all">All genres</option>
          {genres.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <select className="select" value={language} onChange={(e) => setLanguage(e.target.value)}>
          <option value="all">All languages</option>
          {languages.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </select>
        <select className="select" value={rating} onChange={(e) => setRating(e.target.value)}>
          <option value="all">All ratings</option>
          <option value="8">8.0+</option>
          <option value="7">7.0+</option>
        </select>
        <select className="select" value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="popularity">Sort: popularity</option>
          <option value="rating">Sort: rating</option>
        </select>
      </div>
      {loadingMovies ? (
        <Loader />
      ) : list.length ? (
        <MovieGrid movies={list} />
      ) : (
        <EmptyState title="No movies found" message="Try another genre, language, or search term." />
      )}
    </section>
  )
}
