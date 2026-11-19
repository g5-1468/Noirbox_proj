/**
 * Client for the NOIRBOX Java API (java-app, run with serve.bat).
 * All movie, show, seat, booking and review data lives in MySQL.
 */
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, options)
  } catch {
    throw new ApiError(
      'Cannot reach the NOIRBOX server. Start the Java API with java-app\\serve.bat and reload.',
      0,
    )
  }

  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    throw new ApiError(data?.error || `Request failed (HTTP ${response.status})`, response.status)
  }
  return data
}

function postForm(values) {
  return {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(values).toString(),
  }
}

export const api = {
  health: () => request('/health'),

  movies: (params = {}) => request(`/movies?${new URLSearchParams(params)}`),
  movie: (movieId) => request(`/movies/${encodeURIComponent(movieId)}`),

  shows: ({ movieId, date, city }) => {
    const params = { movieId }
    if (date) params.date = date
    if (city) params.city = city
    return request(`/shows?${new URLSearchParams(params)}`)
  },
  show: (showId) => request(`/shows/${showId}`),
  seats: (showId) => request(`/shows/${showId}/seats`),

  book: ({ email, name, showId, seats, method }) =>
    request('/bookings', postForm({ email, name, showId, seats, method })),
  bookings: (email) => request(`/bookings?email=${encodeURIComponent(email)}`),
  booking: (bookingId) => request(`/bookings/${bookingId}`),
  cancelBooking: (bookingId) => request(`/bookings/${bookingId}/cancel`, { method: 'POST' }),

  reviews: (movieId) => request(`/movies/${movieId}/reviews`),
  addReview: (values) => request('/reviews', postForm(values)),
  voteReview: (reviewId) => request(`/reviews/${reviewId}/vote`, { method: 'POST' }),
  deleteReview: (reviewId, email) =>
    request(`/reviews/${reviewId}?email=${encodeURIComponent(email)}`, { method: 'DELETE' }),
}
