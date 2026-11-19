import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import BookingSummary from '../components/BookingSummary'
import Button from '../components/Button'
import EmptyState, { Loader } from '../components/EmptyState'
import { useApp } from '../context/AppContext'
import { getTheatreById } from '../data/theatres'
import { priceBreakdown } from '../utils/bookingUtils'
import { clearDraft } from '../utils/storage'

const METHODS = ['UPI', 'Credit / Debit Card', 'Net Banking']

export default function Checkout() {
  const { draft, user, resetDraft, movies, setLoginModalOpen } = useApp()
  const navigate = useNavigate()

  const [show, setShow] = useState(null)
  const [loading, setLoading] = useState(true)
  const [method, setMethod] = useState(METHODS[0])
  const [upi, setUpi] = useState('')
  const [card, setCard] = useState('')
  const [bank, setBank] = useState('HDFC Bank')
  const [error, setError] = useState('')
  const [seatsTaken, setSeatsTaken] = useState(false)
  const [paying, setPaying] = useState(false)

  useEffect(() => {
    let active = true
    if (!draft.showId) {
      setLoading(false)
      return undefined
    }
    api
      .show(draft.showId)
      .then((data) => active && setShow(data))
      .catch((problem) => active && setError(problem.message))
      .finally(() => active && setLoading(false))

    return () => {
      active = false
    }
  }, [draft.showId])

  if (loading) {
    return (
      <section className="section container">
        <Loader />
      </section>
    )
  }

  if (!show || !draft.seats?.length) {
    return (
      <EmptyState
        title="Nothing to checkout"
        message="Select seats for a show before paying."
        to="/movies"
        cta="Find a movie"
      />
    )
  }

  const movie = movies.find((m) => m.id === show.movieId)
  const theatre = getTheatreById(show.theatreId) || { name: show.theatreName }
  const breakdown = priceBreakdown(show, draft.seats)

  const pay = async (event) => {
    event.preventDefault()

    if (!user) {
      setError('Please log in to complete the booking.')
      return
    }
    if (method === 'UPI' && !upi.includes('@')) {
      setError('Enter a valid UPI ID (demo), e.g. name@okbank.')
      return
    }
    if (method === 'Credit / Debit Card' && card.replace(/\s/g, '').length < 16) {
      setError('Enter a 16-digit demo card number. No charge will be made.')
      return
    }

    setError('')
    setSeatsTaken(false)
    setPaying(true)

    try {
      const booking = await api.book({
        email: user.email,
        name: user.name,
        showId: show.id,
        seats: draft.seats.join(','),
        method,
      })

      clearDraft()
      resetDraft()
      navigate(`/confirmation/${booking.id}`)
    } catch (problem) {
      setPaying(false)
      setError(problem.message)
      if (problem.status === 409) setSeatsTaken(true)
    }
  }

  return (
    <section className="section container">
      <h1 className="page-title">Checkout</h1>
      <p className="muted">Mock payment only. Do not enter real card or UPI details.</p>
      <div className="checkout-grid">
        <form className="panel" onSubmit={pay}>
          <h3>Payment method</h3>
          {METHODS.map((item) => (
            <button
              key={item}
              type="button"
              className={`pay-option ${method === item ? 'active' : ''}`}
              onClick={() => setMethod(item)}
            >
              {item}
            </button>
          ))}
          {method === 'UPI' ? (
            <input className="field" placeholder="yourname@upi" value={upi} onChange={(e) => setUpi(e.target.value)} />
          ) : null}
          {method === 'Credit / Debit Card' ? (
            <input
              className="field"
              placeholder="ACCT-000015"
              value={card}
              onChange={(e) => setCard(e.target.value)}
            />
          ) : null}
          {method === 'Net Banking' ? (
            <select className="select" value={bank} onChange={(e) => setBank(e.target.value)} style={{ width: '100%' }}>
              <option>HDFC Bank</option>
              <option>ICICI Bank</option>
              <option>SBI</option>
              <option>Axis Bank</option>
            </select>
          ) : null}

          {!user ? (
            <p className="error">
              You are not logged in.{' '}
              <button
                type="button"
                className="btn btn-ghost"
                style={{ padding: '2px 8px' }}
                onClick={() => setLoginModalOpen(true)}
              >
                Login
              </button>
            </p>
          ) : null}

          {error ? <p className="error">{error}</p> : null}
          {seatsTaken ? (
            <Button
              type="button"
              variant="ghost"
              style={{ width: '100%', marginTop: 8 }}
              onClick={() => navigate(`/seats/${show.id}`)}
            >
              Pick different seats
            </Button>
          ) : null}

          <Button type="submit" style={{ width: '100%', marginTop: 16 }} disabled={paying}>
            {paying ? 'Processing demo payment…' : `Pay ${breakdown.total.toLocaleString('en-IN')}`}
          </Button>
        </form>
        <BookingSummary movie={movie} theatre={theatre} show={show} seats={draft.seats} breakdown={breakdown} />
      </div>
    </section>
  )
}
