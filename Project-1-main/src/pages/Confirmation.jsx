import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import EmptyState, { Loader } from '../components/EmptyState'
import TicketGraphic from '../components/TicketGraphic'
import { useApp } from '../context/AppContext'
import { getTheatreById } from '../data/theatres'
import { formatINR, formatLongDate } from '../utils/bookingUtils'

export default function Confirmation() {
  const { bookingId } = useParams()
  const { movies } = useApp()
  const [booking, setBooking] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    api
      .booking(bookingId)
      .then((data) => active && setBooking(data))
      .catch((problem) => active && setError(problem.message))
      .finally(() => active && setLoading(false))

    return () => {
      active = false
    }
  }, [bookingId])

  if (loading) {
    return (
      <section className="section container">
        <Loader />
      </section>
    )
  }

  if (!booking) {
    return (
      <EmptyState
        title="Booking not found"
        message={error || 'This booking does not exist in the database.'}
        to="/bookings"
        cta="My bookings"
      />
    )
  }

  const movie = movies.find((m) => m.id === booking.movieId)
  const theatre = getTheatreById(booking.theatreId)

  return (
    <section className="section container">
      <p className="kicker" style={{ textAlign: 'center' }}>
        Payment successful · demo
      </p>
      <h1 className="page-title" style={{ textAlign: 'center' }}>
        You’re in.
      </h1>
      <div className="ticket">
        <div className="ticket-hero" style={{ backgroundImage: `url(${movie?.backdrop})` }} />
        <div className="ticket-body">
          <p className="muted">Booking ID</p>
          <h2>NBX-{String(booking.id).padStart(5, '0')}</h2>
          <h3>{movie?.title || booking.title}</h3>
          <p>
            {theatre?.name || booking.theatreName}
            <br />
            {formatLongDate(booking.date)} · {booking.time}
          </p>
          <p>Seats {booking.seats.join(', ')}</p>
          <p>Total {formatINR(booking.total)} · {booking.method}</p>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'end', marginTop: 16 }}>
            <TicketGraphic bookingId={`NBX-${booking.id}`} />
            <span className="muted">NOIRBOX · stored in MySQL</span>
          </div>
        </div>
      </div>
      <div className="no-print" style={{ display: 'flex', justifyContent: 'center', gap: 12, marginTop: 24 }}>
        <button type="button" className="btn btn-primary" onClick={() => window.print()}>
          Download ticket
        </button>
        <Link to="/bookings" className="btn btn-ghost">
          My bookings
        </Link>
      </div>
    </section>
  )
}
