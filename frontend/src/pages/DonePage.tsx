/**
 * DonePage — shown after the complete double opt-in flow.
 *
 * Swiss High German copy, no sharp s.
 */
export default function DonePage() {
  const pseudonym = sessionStorage.getItem('numnia_child_pseudonym') ?? '';

  return (
    <main>
      <h2>Alles bereit!</h2>
      <p>
        Das Konto ist verifiziert und das Kinderprofil{' '}
        <strong>{pseudonym}</strong> ist aktiv. Sie koennen Ihr Kind jetzt
        anmelden.
      </p>
      <a href="/" data-testid="to-landing">
        Zur Startseite
      </a>
    </main>
  );
}
