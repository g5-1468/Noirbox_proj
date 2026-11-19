import { Outlet } from 'react-router-dom'
import { useApp } from '../context/AppContext'
import Footer from './Footer'
import Navbar from './Navbar'
import { ScrollToTop } from './ScrollToTop'

export default function Layout() {
  const { apiError } = useApp()

  return (
    <div className="page-shell">
      <ScrollToTop />
      {apiError ? (
        <div className="demo-banner" style={{ background: '#5b1d1d', color: '#ffe9e9' }}>
          Live server offline — {apiError}
        </div>
      ) : (
        <div className="demo-banner">
          Live booking system — movies, seats, bookings and reviews are stored in MySQL through the Java API.
        </div>
      )}
      <Navbar />
      <main className="page">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
