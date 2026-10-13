import { useCallback, useEffect, useRef, useState } from 'react';

interface ApiState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Custom data-fetching hook. Interview topics:
 * - race conditions handled with a request counter (stale responses ignored)
 * - why the fetcher must be stable (useCallback in the caller) to avoid loops
 *
 * ODPOWIEDŹ (dlaczego licznik requestów?):
 * Odpowiedzi HTTP mogą przyjść w innej kolejności niż wysłane żądania
 * (np. użytkownik szybko zmienia filtr: request A wysłany przed B, ale
 * odpowiedź A przychodzi PO B). Bez zabezpieczenia stan nadpisałaby
 * nieaktualna (stale) odpowiedź. Każde wywołanie `load` inkrementuje
 * licznik w useRef i zapamiętuje swoje `id`; odpowiedź jest użyta tylko,
 * gdy `id === requestId.current`, czyli gdy to wciąż najnowszy request.
 * Alternatywa: AbortController — faktycznie anuluje żądanie w przeglądarce
 * (cleanup w useEffect wywołuje controller.abort()).
 *
 * ODPOWIEDŹ (dlaczego fetcher musi być stabilny?):
 * `load` zależy od `fetcher`, a useEffect od `load`. Gdyby caller przekazał
 * nową funkcję przy każdym renderze (inline arrow bez useCallback), efekt
 * odpalałby się w pętli: fetch -> setState -> render -> nowy fetcher -> fetch...
 */
export function useApi<T>(fetcher: () => Promise<T>): ApiState<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);

  const load = useCallback(() => {
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    fetcher()
      .then((result) => {
        if (id === requestId.current) {
          setData(result);
        }
      })
      .catch((err: Error) => {
        if (id === requestId.current) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (id === requestId.current) {
          setLoading(false);
        }
      });
  }, [fetcher]);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, refetch: load };
}
