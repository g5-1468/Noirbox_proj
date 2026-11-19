/**
 * Only UI preferences and the demo login stay in the browser now.
 * Bookings, seats and reviews are stored in MySQL through the Java API.
 */
const KEYS = {
  draft: 'noirbox_draft',
  city: 'noirbox_city',
  user: 'noirbox_user',
  votes: 'noirbox_review_votes',
}

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // storage can be disabled - the app still works
  }
}

export function getDraft() {
  return read(KEYS.draft, null)
}

export function saveDraft(draft) {
  write(KEYS.draft, draft)
}

export function clearDraft() {
  localStorage.removeItem(KEYS.draft)
}

export function getStoredCity() {
  return localStorage.getItem(KEYS.city)
}

export function setStoredCity(city) {
  localStorage.setItem(KEYS.city, city)
}

export function getStoredUser() {
  return read(KEYS.user, null)
}

export function setStoredUser(user) {
  write(KEYS.user, user)
}

export function clearStoredUser() {
  localStorage.removeItem(KEYS.user)
}

// Reviews the user already marked as helpful (one vote per browser).
export function getUserVotedReviewIds() {
  return read(KEYS.votes, [])
}

export function markReviewVoted(reviewId) {
  const ids = new Set(getUserVotedReviewIds())
  ids.add(reviewId)
  write(KEYS.votes, [...ids])
}

export function hasVotedForReview(reviewId) {
  return getUserVotedReviewIds().includes(reviewId)
}

/**
 * Decision engine: turns the reviews fetched from the API into the
 * audience verdict card shown on the movie page.
 */
export function calculateMovieVerdict(reviews, baselineScore = 8.0) {
  if (!reviews || reviews.length === 0) {
    return {
      totalCount: 0,
      avgRating: baselineScore,
      recommendPercent: 85,
      verdictBadge: {
        label: 'Must Watch in Theatres',
        emoji: '🏆',
        type: 'must-watch',
      },
      distribution: { '9-10': 60, '7-8': 30, '5-6': 8, '3-4': 2, '1-2': 0 },
      topTags: ['Cinematography', 'Sound Design', 'Acting'],
    }
  }

  const totalCount = reviews.length
  const totalScore = reviews.reduce((sum, r) => sum + (Number(r.rating) || 0), 0)
  const avgRating = Number((totalScore / totalCount).toFixed(1))

  const recommendedCount = reviews.filter(
    (r) => r.verdict === 'must-watch' || r.verdict === 'worth-it' || r.rating >= 7,
  ).length
  const recommendPercent = Math.round((recommendedCount / totalCount) * 100)

  let verdictBadge = {
    label: 'Worth a Watch',
    emoji: '🎟️',
    type: 'worth-it',
  }

  if (recommendPercent >= 75 && avgRating >= 7.6) {
    verdictBadge = {
      label: 'Must Watch in Theatres',
      emoji: '🏆',
      type: 'must-watch',
    }
  } else if (recommendPercent < 55 || avgRating < 6.0) {
    verdictBadge = {
      label: 'Mixed · Wait for OTT',
      emoji: '🛋️',
      type: 'skip',
    }
  }

  const distCounts = { '9-10': 0, '7-8': 0, '5-6': 0, '3-4': 0, '1-2': 0 }
  reviews.forEach((r) => {
    const score = Number(r.rating)
    if (score >= 9) distCounts['9-10'] += 1
    else if (score >= 7) distCounts['7-8'] += 1
    else if (score >= 5) distCounts['5-6'] += 1
    else if (score >= 3) distCounts['3-4'] += 1
    else distCounts['1-2'] += 1
  })

  const tagCounts = {}
  reviews.forEach((r) => {
    ;(r.tags || []).forEach((t) => {
      tagCounts[t] = (tagCounts[t] || 0) + 1
    })
  })
  const topTags = Object.entries(tagCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([t]) => t)

  return {
    totalCount,
    avgRating,
    recommendPercent,
    verdictBadge,
    distribution: distCounts,
    topTags: topTags.length > 0 ? topTags : ['Great Visuals', 'Performances'],
  }
}
