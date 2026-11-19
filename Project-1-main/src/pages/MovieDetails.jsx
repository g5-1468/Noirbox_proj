import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import Button from '../components/Button'
import EmptyState, { Loader } from '../components/EmptyState'
import MovieReviewsSection from '../components/Reviews/MovieReviewsSection'
import { useApp } from '../context/AppContext'
import { getTheatreById } from '../data/theatres'
import usePolling from '../hooks/usePolling'
import { formatDuration } from '../utils/bookingUtils'
import { calculateMovieVerdict } from '../utils/storage'

// Keep the verdict chip above in step with reviews written on other devices.
const REVIEW_REFRESH_MS = 15000

export default function MovieDetails() {
  const { movieId } = useParams()
  const { city, updateDraft, movies, loadingMovies } = useApp()
  const navigate = useNavigate()

  const movie = movies.find((m) => m.id === movieId)

  const [reviews, setReviews] = useState([])
  const [cityShows, setCityShows] = useState([])

  const loadReviews = useCallback(() => {
    if (!movie?.id) return
    api
      .reviews(movie.id)
      .then(setReviews)
      .catch(() => {})
  }, [movie?.id])

  useEffect(() => {
    loadReviews()
  }, [loadReviews])

  usePolling(loadReviews, REVIEW_REFRESH_MS)

  useEffect(() => {
    let active = true
    if (!movie) return undefined

    api
      .shows({ movieId: movie.id, city })
      .then((list) => active && setCityShows(list))
      .catch(() => active && setCityShows([]))

    return () => {
      active = false
    }
  }, [movie, city])

  const verdictData = useMemo(
    () => calculateMovieVerdict(reviews, movie?.rating),
    [reviews, movie],
  )

  if (loadingMovies && !movie) {
    return (
      <section className="section container">
        <Loader />
      </section>
    )
  }

  if (!movie) {
    return (
      <EmptyState
        title="Movie not found"
        message="This title is not in the catalogue."
        to="/movies"
        cta="Browse movies"
      />
    )
  }

  const theatreCount = new Set(cityShows.map((s) => s.theatreId)).size
  const canBook = movie.status === 'now-showing' && cityShows.length > 0

  const book = () => {
    updateDraft({ movieId: movie.id, seats: [] })
    navigate(`/movies/${movie.id}/showtimes`)
  }

  const scrollToReviews = () => {
    const el = document.getElementById('reviews')
    if (el) el.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <div className="movie-details-page">
      <section className="container details">
        <div className="details-poster">
          <img src={movie.poster} alt={movie.title} />
        </div>
        <div>
          <p className="kicker">
            {movie.status === 'now-showing' ? 'Now showing' : 'Coming soon'}
          </p>
          <h1 className="page-title">{movie.title}</h1>
          <p className="muted">{movie.tagline}</p>

          <div className="meta-row">
            {movie.rating > 0 ? (
              <span className="chip" style={{ color: 'var(--gold)' }}>
                ★ {verdictData.avgRating}
              </span>
            ) : (
              <span className="chip">Unrated</span>
            )}
            <button
              type="button"
              className="chip chip-verdict-jump"
              onClick={scrollToReviews}
              title="Click to view audience reviews"
            >
              {verdictData.verdictBadge.emoji} {verdictData.recommendPercent}% Recommend · {verdictData.totalCount} reviews
            </button>
            {movie.genres.map((g) => (
              <span className="chip" key={g}>
                {g}
              </span>
            ))}
            <span className="chip">{movie.language}</span>
            {movie.duration ? <span className="chip">{formatDuration(movie.duration)}</span> : null}
            <span className="chip">{movie.certification}</span>
          </div>

          <p>
            <strong>Release</strong> · {movie.releaseDate} &nbsp;|&nbsp;{' '}
            <strong>Director</strong> · {movie.director}
          </p>
          <p>{movie.description}</p>

          <h3>Cast</h3>
          <div className="cast-list">
            {movie.cast.map((person) => (
              <span className="cast-pill" key={person}>
                {person}
              </span>
            ))}
          </div>

          <div style={{ display: 'flex', gap: 12, marginTop: 24, flexWrap: 'wrap' }}>
            {movie.trailerUrl ? (
              <a
                className="btn btn-primary"
                href={movie.trailerUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                Watch trailer
              </a>
            ) : null}
            <Button onClick={book} disabled={!canBook}>
              Book tickets
            </Button>
            <Button variant="ghost" onClick={scrollToReviews}>
              Read Reviews ({verdictData.totalCount})
            </Button>
          </div>

          {!canBook ? (
            <p className="muted" style={{ marginTop: 12 }}>
              {movie.status === 'coming-soon'
                ? 'Advance booking opens closer to release.'
                : 'No theatres are listing this title in the selected city yet.'}
            </p>
          ) : (
            <p className="muted" style={{ marginTop: 12 }}>
              Playing at {theatreCount} theatre{theatreCount === 1 ? '' : 's'} in this city.{' '}
              <Link to={`/movies/${movie.id}/showtimes`}>See showtimes</Link>
            </p>
          )}

          {cityShows.length > 0 ? (
            <div className="panel" style={{ marginTop: 24 }}>
              <h3>Tonight in this city</h3>
              <p className="muted">
                {Array.from(
                  new Set(cityShows.map((s) => getTheatreById(s.theatreId)?.name)),
                )
                  .slice(0, 4)
                  .join(' · ')}
              </p>
            </div>
          ) : null}
        </div>
      </section>

      <div className="container" style={{ marginTop: 48, marginBottom: 64 }}>
        <MovieReviewsSection movie={movie} />
      </div>
    </div>
  )
}
