import { useEffect, useRef } from 'react'

/**
 * Re-runs `callback` on an interval while the tab is visible, so a page picks up
 * changes other people made on their own devices without a manual reload.
 */
export default function usePolling(callback, intervalMs) {
  const saved = useRef(callback)

  useEffect(() => {
    saved.current = callback
  }, [callback])

  useEffect(() => {
    if (!intervalMs) return undefined

    const tick = () => {
      if (document.visibilityState === 'visible') saved.current()
    }

    const timer = window.setInterval(tick, intervalMs)
    document.addEventListener('visibilitychange', tick)

    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', tick)
    }
  }, [intervalMs])
}
