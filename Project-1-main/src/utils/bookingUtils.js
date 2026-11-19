const ROWS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J']

export const SEAT_LAYOUT = {
  premium: { rows: ['A', 'B'], seats: 12, label: 'Premium' },
  executive: { rows: ['C', 'D', 'E', 'F'], seats: 14, label: 'Executive' },
  normal: { rows: ['G', 'H', 'I', 'J'], seats: 14, label: 'Normal' },
}

export const CATEGORY_MULTIPLIER = {
  premium: 1.55,
  executive: 1.2,
  normal: 1,
}

export const CONVENIENCE_FEE_PER_TICKET = 28

export function getSeatCategory(row) {
  if (SEAT_LAYOUT.premium.rows.includes(row)) return 'premium'
  if (SEAT_LAYOUT.executive.rows.includes(row)) return 'executive'
  return 'normal'
}

export function getSeatPrice(basePrice, seatId) {
  const row = seatId[0]
  return Math.round(basePrice * CATEGORY_MULTIPLIER[getSeatCategory(row)])
}

export function buildSeatMap() {
  return ROWS.map((row) => {
    const category = getSeatCategory(row)
    const count = SEAT_LAYOUT[category].seats
    const seats = Array.from({ length: count }, (_, i) => {
      const number = i + 1
      return {
        id: `${row}${number}`,
        row,
        number,
        category,
      }
    })
    return { row, category, seats }
  })
}

export function formatINR(amount, digits = 0) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  }).format(amount)
}

export function formatDuration(mins) {
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return `${h}h ${m}m`
}

export function formatLongDate(iso) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString('en-IN', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

export function priceBreakdown(show, seatIds) {
  const tickets = seatIds.map((id) => ({
    id,
    price: getSeatPrice(show.price, id),
    category: getSeatCategory(id[0]),
  }))
  const subtotal = tickets.reduce((sum, t) => sum + t.price, 0)
  const convenienceFee = seatIds.length * CONVENIENCE_FEE_PER_TICKET
  const total = subtotal + convenienceFee
  return { tickets, subtotal, convenienceFee, total, count: seatIds.length }
}

export { ROWS }
