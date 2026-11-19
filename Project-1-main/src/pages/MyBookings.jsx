import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import EmptyState, { Loader } from '../components/EmptyState'
import TicketGraphic from '../components/TicketGraphic'
import { useApp } from '../context/AppContext'
import { getTheatreById } from '../data/theatres'
import { formatINR, formatLongDate } from '../utils/bookingUtils'

export default function MyBookings() {
  const { user, movies, setLoginModalOpen } = useApp()
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [active, setActive] = useState(null)
  const [cancelling, setCancelling] = useState(0)

  const load = useCallback(() => {
    if (!user) {
      setBookings([])
      setLoading(false)
      return
    }
    setLoading(true)
    api
      .bookings(user.email)
      .then((list) => {
        setBookings(list)
        setError('')
      })
      .catch((problem) => setError(problem.message))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => {
    load()
  }, [load])

  const cancel = async (bookingId) => {
    if (!window.confirm('Cancel this booking? Seats are released back to the show.')) return
    setCancelling(bookingId)
    try {
      await api.cancelBooking(bookingId)
      load()
      setActive(null)
    } catch (problem) {
      setError(problem.message)
    } finally {
      setCancelling(0)
    }
  }

  if (!user) {
    return (
      <section className="section container">
        <EmptyState
          title="Sign in to see your bookings"
          message="Your tickets are stored in the database under your email address."
          to="/"
          cta="Go home"
        />
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 16 }}>
          <button type="button" className="btn btn-primary" onClick={() => setLoginModalOpen(true)}>
            Login
          </button>
        </div>
      </section>
    )
  }

  if (loading) {
    return (
      <section className="section container">
        <Loader />
      </section>
    )
  }

  if (!bookings.length) {
    return (
      <section className="section container">
        <EmptyState
          title="No bookings yet"
          message="Book a show and it will appear here, loaded from MySQL."
          to="/movies"
          cta="Browse movies"
        />
        {error ? <p className="error">{error}</p> : null}
      </section>
    )
  }

  const selected = bookings.find((b) => b.id === active)

  return (
    <section className="section container">
      <h1 className="page-title">My bookings</h1>
      <p className="muted">Loaded from MySQL for {user.email}. Cancelling a booking frees its seats.</p>
      {error ? <p className="error">{error}</p> : null}
      <div className="theatre-list" style={{ marginTop: 24 }}>
        {bookings.map((booking) => {
          const movie = movies.find((m) => m.id === booking.movieId)
          const theatre = getTheatreById(booking.theatreId)
          return (
            <article key={booking.id} className="booking-card">
              <div className="theatre-top">
                <div>
                  <h3 style={{ margin: 0 }}>{movie?.title || booking.title}</h3>
                  <p className="muted">
                    {theatre?.name || booking.theatreName} · {formatLongDate(booking.date)} · {booking.time}
                  </p>
                  <p>
                    Seats {booking.seats.join(', ')} · {formatINR(booking.total)} ·{' '}
                    <strong>{booking.status}</strong>
                  </p>
                  <p className="muted">NBX-{String(booking.id).padStart(5, '0')}</p>
                </div>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <button type="button" className="btn btn-ghost" onClick={() => setActive(booking.id)}>
                    View ticket
                  </button>
                  {booking.status === 'CONFIRMED' ? (
                    <button
                      type="button"
                      className="btn btn-ghost"
                      onClick={() => cancel(booking.id)}
                      disabled={cancelling === booking.id}
                    >
                      {cancelling === booking.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  ) : null}
                </div>
              </div>
            </article>
          )
        })}
      </div>

      {selected ? (
        <div className="modal-backdrop" onClick={() => setActive(null)} role="presentation">
          <div className="ticket" onClick={(e) => e.stopPropagation()} style={{ width: 'min(520px, 100%)' }}>
            <div
              className="ticket-hero"
              style={{ backgroundImage: `url(${movies.find((m) => m.id === selected.movieId)?.backdrop})` }}
            />
            <div className="ticket-body">
              <h2>NBX-{String(selected.id).padStart(5, '0')}</h2>
              <h3>{movies.find((m) => m.id === selected.movieId)?.title || selected.title}</h3>
              <p>
                {getTheatreById(selected.theatreId)?.name || selected.theatreName}
                <br />
                {formatLongDate(selected.date)} · {selected.time}
              </p>
              <p>Seats {selected.seats.join(', ')}</p>
              <p>
                {formatINR(selected.total)} · {selected.status}
              </p>
              <TicketGraphic bookingId={`NBX-${selected.id}`} />
              <div style={{ display: 'flex', gap: 8, marginTop: 16 }} className="no-print">
                <button type="button" className="btn btn-primary" onClick={() => window.print()}>
                  Download ticket
                </button>
                <Link to={`/confirmation/${selected.id}`} className="btn btn-ghost">
                  Open page
                </Link>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
