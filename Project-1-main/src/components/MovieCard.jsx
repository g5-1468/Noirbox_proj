import { Link } from 'react-router-dom'
import { formatINR } from '../utils/bookingUtils'

const VERDICTS = [
  { type: 'must-watch', emoji: '🏆', label: 'Must Watch in Theatres' },
  { type: 'worth-it', emoji: '🎟️', label: 'Worth a Watch' },
  { type: 'skip', emoji: '🛋️', label: 'Mixed · Wait for OTT' },
]

export default function MovieCard({ movie }) {
  const recommend = movie.recommendPercent || 0
  const verdict =
    recommend >= 75 ? VERDICTS[0] : recommend >= 55 ? VERDICTS[1] : recommend > 0 ? VERDICTS[2] : null
  const rating = movie.audienceRating > 0 ? movie.audienceRating : movie.rating

  return (
    <Link to={`/movies/${movie.id}`} className="movie-card">
      <div className="poster-wrap">
        <img src={movie.poster} alt={movie.title} />
        {rating > 0 ? (
          <span className="rating-badge">★ {rating}</span>
        ) : (
          <span className="rating-badge">Soon</span>
        )}
        {movie.status === 'now-showing' && verdict ? (
          <span className="recommend-badge" title="Audience Recommendation Rate">
            {verdict.emoji} {recommend}%
          </span>
        ) : null}
      </div>
      <div className="card-body">
        <h3>{movie.title}</h3>
        <div className="card-meta">
          {movie.genres.join(' / ')} · {movie.language}
        </div>
        {movie.status === 'now-showing' ? (
          <div className="card-footer-row">
            <span className="card-price">From {formatINR(220)}</span>
            <span className="card-verdict-text">{verdict ? verdict.label : 'New release'}</span>
          </div>
        ) : null}
      </div>
    </Link>
  )
}
