/**
 * App — application shell with React Router 7 routes for UC-001 and UC-002.
 *
 * UI copy is Swiss High German with umlauts, without sharp s (NFR-I18N-002,
 * NFR-I18N-004). Business screens are added per UC implementation.
 */
import { Routes, Route } from 'react-router-dom';
import RegisterPage from './pages/RegisterPage';
import CheckEmailPage from './pages/CheckEmailPage';
import VerifyPage from './pages/VerifyPage';
import ChildProfilePage from './pages/ChildProfilePage';
import OnboardingCheckEmailPage from './pages/OnboardingCheckEmailPage';
import ConfirmChildPage from './pages/ConfirmChildPage';
import DonePage from './pages/DonePage';
import ChildSignInPage from './pages/ChildSignInPage';
import ChildLockedPage from './pages/ChildLockedPage';
import ChildShellPage from './pages/ChildShellPage';
import ParentDashboardPage from './pages/ParentDashboardPage';
import TrainingPage from './pages/TrainingPage';
import AccuracyPage from './pages/AccuracyPage';
import WorldMapPage from './pages/WorldMapPage';
import GalleryPage from './pages/GalleryPage';
import AvatarPage from './pages/AvatarPage';
import ShopPage from './pages/ShopPage';
import ProgressPage from './pages/ProgressPage';
import ParentControlsPage from './pages/ParentControlsPage';
import ExportPage from './pages/ExportPage';
import DeletionPage from './pages/DeletionPage';

export default function App() {
  return (
    <>
      <header>
        <h1>Numnia – spielerisch rechnen lernen</h1>
      </header>

      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/register/check-email" element={<CheckEmailPage />} />
        <Route path="/verify" element={<VerifyPage />} />
        <Route path="/onboarding/child" element={<ChildProfilePage />} />
        <Route path="/onboarding/check-email" element={<OnboardingCheckEmailPage />} />
        <Route path="/onboarding/confirm" element={<ConfirmChildPage />} />
        <Route path="/onboarding/done" element={<DonePage />} />
        {/* UC-002: child sign-in */}
        <Route path="/sign-in/child" element={<ChildSignInPage />} />
        <Route path="/sign-in/child/locked" element={<ChildLockedPage />} />
        <Route path="/child" element={<ChildShellPage />} />
        <Route path="/parents/me" element={<ParentDashboardPage />} />
        {/* UC-003: Training mode */}
        <Route path="/training" element={<TrainingPage />} />
        <Route path="/accuracy" element={<AccuracyPage />} />
        {/* UC-005: World map */}
        <Route path="/worlds" element={<WorldMapPage />} />
        {/* UC-006: Creatures gallery */}
        <Route path="/gallery" element={<GalleryPage />} />
        {/* UC-007: Avatar customization and shop */}
        <Route path="/avatar" element={<AvatarPage />} />
        <Route path="/shop" element={<ShopPage />} />
        {/* UC-008: Progress view */}
        <Route path="/progress" element={<ProgressPage />} />
        {/* UC-009: Parent daily limit + risk controls */}
        <Route path="/parents/controls/:childId" element={<ParentControlsPage />} />
        {/* UC-010: Parent data export (JSON / PDF) */}
        <Route path="/parents/exports/:childId" element={<ExportPage />} />
        <Route path="/parents/deletion/:childId" element={<DeletionPage />} />
      </Routes>
    </>
  );
}

function LandingPage() {
  return (
    <main>
      <h2>Willkommen bei Numnia!</h2>
      <p>Lernspiele fuer Kinder von 7 bis 12 Jahren.</p>
      <a href="/register">Jetzt registrieren</a>
    </main>
  );
}
