import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import BookingSummary from '../components/BookingSummary'
import Button from '../components/Button'
import EmptyState, { Loader } from '../components/EmptyState'
import SeatMap from '../components/SeatMap'
import { useApp } from '../context/AppContext'
import { getTheatreById } from '../data/theatres'
import { priceBreakdown } from '../utils/bookingUtils'

export default function SeatSelection() {
  const { showId } = useParams()
  const { draft, updateDraft, movies } = useApp()
  const navigate = useNavigate()

  const [show, setShow] = useState(null)
  const [seatData, setSeatData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Live show details + seats already booked in MySQL.
  useEffect(() => {
    let active = true
    setLoading(true)

    Promise.all([api.show(showId), api.seats(showId)])
      .then(([showData, seats]) => {
        if (!active) return
        setShow(showData)
        setSeatData(seats)
        setError('')
      })
      .catch((problem) => {
        if (!active) return
        setError(problem.message)
        setShow(null)
      })
      .finally(() => active && setLoading(false))

    return () => {
      active = false
    }
  }, [showId])

  useEffect(() => {
    if (!show) return
    if (draft.showId !== show.id) {
      updateDraft({
        movieId: show.movieId,
        theatreId: show.theatreId,
        showId: show.id,
        date: show.date,
        seats: [],
      })
    }
  }, [show, draft.showId, updateDraft])

  const occupied = useMemo(
    () => new Set(seatData?.booked ?? []),
    [seatData],
  )

  if (loading) {
    return (
      <section className="section container">
        <Loader />
      </section>
    )
  }

  if (!show) {
    return (
      <EmptyState
        title="Show not found"
        message={error || 'This showtime is no longer available.'}
        to="/movies"
        cta="Browse movies"
      />
    )
  }

  const movie = movies.find((m) => m.id === show.movieId)
  const theatre = getTheatreById(show.theatreId)
  const selected = draft.showId === show.id ? draft.seats : []
  const breakdown = priceBreakdown(show, selected)

  const toggle = (seatId) => {
    const next = selected.includes(seatId)
      ? selected.filter((id) => id !== seatId)
      : [...selected, seatId].sort()
    updateDraft({
      movieId: show.movieId,
      theatreId: show.theatreId,
      showId: show.id,
      date: show.date,
      seats: next,
    })
  }

  return (
    <section className="section container">
      <p className="kicker">{theatre?.name || show.theatreName}</p>
      <h1 className="page-title">Select seats</h1>
      <p className="muted">
        {movie?.title} · {show.date} · {show.time} · {show.format}
      </p>
      <p className="muted">
        {seatData.seatsAvailable} of {seatData.totalSeats} seats available · Premium{' '}
        {seatData.price.premium} · Executive {seatData.price.executive} · Normal{' '}
        {seatData.price.normal}
      </p>
      <div className="seat-layout">
        <div className="panel">
          <SeatMap occupied={occupied} selected={selected} onToggle={toggle} />
        </div>
        <BookingSummary
          movie={movie}
          theatre={theatre}
          show={show}
          seats={selected}
          breakdown={breakdown}
          action={
            <>
              {selected.length === 0 ? <p className="error">Select at least one available seat to continue.</p> : null}
              <Button
                style={{ width: '100%', marginTop: 12 }}
                disabled={selected.length === 0}
                onClick={() => navigate('/checkout')}
              >
                Proceed to checkout
              </Button>
            </>
          }
        />
      </div>
    </section>
  )
}
