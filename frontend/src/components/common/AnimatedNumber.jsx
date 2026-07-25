import { useEffect, useRef, useState } from "react";

const prefersReducedMotion = () =>
  typeof window !== "undefined" &&
  window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;

// easeOutExpo — starts fast, settles gently near the target instead of a linear tick-up
const ease = (t) => (t >= 1 ? 1 : 1 - Math.pow(2, -10 * t));

function AnimatedNumber({ value, duration = 900, delay = 0, format = (n) => n }) {
  const target = Number(value) || 0;
  const [display, setDisplay] = useState(() =>
    prefersReducedMotion() ? target : 0,
  );
  const fromRef = useRef(prefersReducedMotion() ? target : 0);
  const rafRef = useRef(null);
  const timeoutRef = useRef(null);

  useEffect(() => {
    const from = fromRef.current;
    const to = target;
    if (from === to || prefersReducedMotion()) {
      setDisplay(to);
      fromRef.current = to;
      return;
    }
    cancelAnimationFrame(rafRef.current);
    clearTimeout(timeoutRef.current);
    const tick = (start) => (now) => {
      const progress = Math.min(1, (now - start) / duration);
      setDisplay(Math.round(from + (to - from) * ease(progress)));
      if (progress < 1) {
        rafRef.current = requestAnimationFrame(tick(start));
      } else {
        fromRef.current = to;
      }
    };
    timeoutRef.current = setTimeout(() => {
      rafRef.current = requestAnimationFrame(tick(performance.now()));
    }, delay);
    return () => {
      clearTimeout(timeoutRef.current);
      cancelAnimationFrame(rafRef.current);
    };
  }, [target, duration, delay]);

  return format(display);
}

export default AnimatedNumber;
