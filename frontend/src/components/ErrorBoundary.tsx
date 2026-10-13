import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/**
 * Error boundaries must be class components (interview question!) -
 * there is no hook equivalent for componentDidCatch yet.
 *
 * ODPOWIEDŹ: Error boundary musi być klasą, bo opiera się na dwóch metodach
 * cyklu życia, które nie mają odpowiedników w hookach:
 * - static getDerivedStateFromError(error) — wywoływana w fazie renderowania,
 *   służy TYLKO do ustawienia stanu (wyrenderowania UI awaryjnego); musi być
 *   czysta (bez efektów ubocznych).
 * - componentDidCatch(error, info) — wywoływana w fazie commit, tu można
 *   logować błąd (np. do Sentry); info.componentStack pokazuje, który
 *   komponent rzucił wyjątek.
 * Warto też wiedzieć, czego error boundary NIE łapie:
 * - błędów w event handlerach (tam używamy zwykłego try/catch),
 * - błędów w kodzie asynchronicznym (setTimeout, promisy),
 * - błędów rzuconych w samym error boundary,
 * - błędów podczas SSR.
 * W praktyce często używa się biblioteki react-error-boundary, która
 * opakowuje ten wzorzec w wygodne API z funkcyjnym komponentem.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Uncaught error:', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="error-boundary">
          <h2>Something went wrong</h2>
          <p>{this.state.error.message}</p>
          <button onClick={() => this.setState({ error: null })}>Try again</button>
        </div>
      );
    }
    return this.props.children;
  }
}
