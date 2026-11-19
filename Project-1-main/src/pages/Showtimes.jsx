import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import EmptyState, { Loader } from '../components/EmptyState'
import TheatreCard from '../components/TheatreCard'
import { useApp } from '../context/AppContext'
import { locations } from '../data/locations'
import { getBookingDates } from '../data/shows'

export default function Showtimes() {
  const { movieId } = useParams()
  const { city, draft, updateDraft, movies, loadingMovies } = useApp()
  const navigate = useNavigate()
  const dates = useMemo(() => getBookingDates(), [])
  const movie = movies.find((m) => m.id === movieId)

  const selectedDate = draft.date || dates[0]?.iso
  const [shows, setShows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!movie || !selectedDate) return undefined
    let active = true
    setLoading(true)

    api
      .shows({ movieId: movie.id, date: selectedDate, city })
      .then((list) => {
        if (!active) return
        setShows(list)
        setError('')
      })
      .catch((problem) => {
        if (!active) return
        setShows([])
        setError(problem.message)
      })
      .finally(() => active && setLoading(false))

    return () => {
      active = false
    }
  }, [movie, city, selectedDate])

  const grouped = useMemo(() => {
    const map = new Map()
    shows.forEach((show) => {
      if (!map.has(show.theatreId)) map.set(show.theatreId, [])
      map.get(show.theatreId).push(show)
    })
    return [...map.entries()]
  }, [shows])

  if (loadingMovies && !movie) {
    return (
      <section className="section container">
        <Loader />
      </section>
    )
  }

  if (!movie) {
    return <EmptyState title="Movie not found" to="/movies" cta="Back to movies" />
  }

  if (movie.status !== 'now-showing') {
    return (
      <EmptyState
        title="Not open for booking"
        message="This title is coming soon."
        to={`/movies/${movie.id}`}
        cta="View details"
      />
    )
  }

  const cityName = locations.find((l) => l.id === city)?.name

  const selectShow = (show) => {
    updateDraft({
      movieId: movie.id,
      theatreId: show.theatreId,
      showId: show.id,
      date: show.date,
      seats: [],
    })
  }

  const goToSeats = () => {
    if (draft.showId) navigate(`/seats/${draft.showId}`)
  }

  return (
    <section className="section container">
      <p className="kicker">{cityName}</p>
      <h1 className="page-title">{movie.title}</h1>
      <p className="muted">
        Choose a date, theatre, format and showtime. Ticket prices vary by screen and seat category.
      </p>
      <div className="date-strip">
        {dates.map((d) => (
          <button
            key={d.iso}
            type="button"
            className={`date-chip ${selectedDate === d.iso ? 'active' : ''}`}
            onClick={() => updateDraft({ date: d.iso, showId: null, seats: [] })}
          >
            <div>{d.label}</div>
            <strong>
              {d.day} {d.month}
            </strong>
          </button>
        ))}
      </div>

      {error ? <p className="error">{error}</p> : null}
      {loading ? (
        <Loader />
      ) : grouped.length === 0 ? (
        <EmptyState
          title="No shows in this city"
          message="Switch location from the navbar or pick another date."
        />
      ) : (
        <div className="theatre-list">
          {grouped.map(([theatreId, theatreShows]) => (
            <TheatreCard
              key={theatreId}
              theatreId={theatreId}
              shows={theatreShows}
              selectedShowId={draft.showId}
              onSelectShow={selectShow}
              onSelectSeats={goToSeats}
            />
          ))}
        </div>
      )}
    </section>
  )
}
