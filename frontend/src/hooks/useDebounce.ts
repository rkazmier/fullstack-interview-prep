import { useEffect, useState } from 'react';

/**
 * Custom hook: returns the value only after it stopped changing for `delay` ms.
 * Demonstrates useEffect cleanup - a classic interview question.
 *
 * ODPOWIEDŹ (jak działa cleanup w useEffect):
 * Funkcja zwrócona z useEffect uruchamia się:
 * 1) przed KAŻDYM kolejnym wykonaniem efektu (gdy zmienią się zależności),
 * 2) przy odmontowaniu komponentu.
 * Tutaj: każda zmiana `value` kasuje poprzedni timer i ustawia nowy, więc
 * `debounced` zaktualizuje się dopiero, gdy wartość przestanie się zmieniać
 * przez `delay` ms. Bez cleanupu mielibyśmy wyciek timerów i aktualizacje
 * stanu po odmontowaniu. Typowe zastosowanie: ograniczenie liczby zapytań
 * do API podczas wpisywania w pole wyszukiwania.
 */
export function useDebounce<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer); // cleanup cancels the previous timer
  }, [value, delay]);

  return debounced;
}
