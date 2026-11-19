import { useState } from 'react'
import { api } from '../../api'
import { hasVotedForReview, markReviewVoted } from '../../utils/storage'

export default function ReviewCard({ review, currentUser, onDelete, onVoteChange }) {
  const [showSpoiler, setShowSpoiler] = useState(false)
  const [likesCount, setLikesCount] = useState(review.likes || 0)
  const [hasVoted, setHasVoted] = useState(hasVotedForReview(review.reviewId))
  const [voting, setVoting] = useState(false)

  const handleVote = async () => {
    if (hasVoted || voting) return
    setVoting(true)
    try {
      const result = await api.voteReview(review.reviewId)
      markReviewVoted(review.reviewId)
      setHasVoted(true)
      setLikesCount(result.likes)
      if (onVoteChange) onVoteChange()
    } catch {
      // keep the previous count when the server is unreachable
    } finally {
      setVoting(false)
    }
  }

  const isAuthor =
    currentUser &&
    ((review.authorEmail && currentUser.email === review.authorEmail) ||
      currentUser.name === review.authorName)

  const verdictConfig = {
    'must-watch': { label: 'Must Watch', emoji: '🏆', className: 'verdict-must-watch' },
    'worth-it': { label: 'Worth It', emoji: '🎟️', className: 'verdict-worth-it' },
    skip: { label: 'Skip / OTT', emoji: '🛋️', className: 'verdict-skip' },
  }
  const verdict = verdictConfig[review.verdict] || verdictConfig['worth-it']

  const initials = (review.authorName || 'U')
    .split(' ')
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)

  return (
    <article className="review-card">
      <div className="review-card-header">
        <div className="review-author-info">
          <div className="review-avatar" aria-hidden="true">
            {initials}
          </div>
          <div>
            <div className="review-author-row">
              <span className="review-author-name">{review.authorName}</span>
              {review.verifiedBooking && (
                <span className="review-verified-badge" title="Verified Ticket Holder">
                  ✓ Verified Viewer
                </span>
              )}
            </div>
            <span className="review-date">{review.date}</span>
          </div>
        </div>

        <div className="review-badges-row">
          <div className="review-rating-chip">★ {review.rating}<span>/10</span></div>
          <span className={`review-verdict-pill ${verdict.className}`}>
            {verdict.emoji} {verdict.label}
          </span>
        </div>
      </div>

      <h4 className="review-headline">{review.headline}</h4>

      {review.hasSpoilers && !showSpoiler ? (
        <div className="review-spoiler-box">
          <span className="review-spoiler-warning">⚠️ Contains spoilers</span>
          <button
            type="button"
            className="btn btn-ghost review-spoiler-btn"
            onClick={() => setShowSpoiler(true)}
          >
            Click to reveal review
          </button>
        </div>
      ) : (
        <p className="review-content">{review.content}</p>
      )}

      {review.tags && review.tags.length > 0 && (
        <div className="review-tags-row">
          {review.tags.map((tag) => (
            <span key={tag} className="review-tag-chip">
              #{tag}
            </span>
          ))}
        </div>
      )}

      <div className="review-card-footer">
        <button
          type="button"
          className={`review-helpful-btn ${hasVoted ? 'voted' : ''}`}
          onClick={handleVote}
          title={hasVoted ? 'You already marked this review helpful' : 'Mark this review as helpful'}
          disabled={hasVoted || voting}
        >
          <span className="helpful-icon">👍</span>
          <span>Helpful ({likesCount})</span>
        </button>

        {isAuthor && onDelete && (
          <button
            type="button"
            className="review-delete-btn"
            onClick={() => onDelete(review.reviewId)}
            title="Delete your review"
          >
            Delete
          </button>
        )}
      </div>
    </article>
  )
}
