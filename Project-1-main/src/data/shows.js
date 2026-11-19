/** Dates offered in the showtime date strip (today + 4 days). */
export function getBookingDates(days = 5) {
  const dates = []
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  for (let i = 0; i < days; i += 1) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
      d.getDate(),
    ).padStart(2, '0')}`
    dates.push({
      iso,
      label: d.toLocaleDateString('en-IN', { weekday: 'short' }),
      day: d.getDate(),
      month: d.toLocaleDateString('en-IN', { month: 'short' }),
    })
  }
  return dates
}
