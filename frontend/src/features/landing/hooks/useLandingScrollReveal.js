import { useEffect, useRef } from "react";

function revealAll(elements) {
  elements.forEach((element) => {
    element.classList.add("is-revealed");
  });
}

export function useLandingScrollReveal() {
  const landingPageRef = useRef(null);

  useEffect(() => {
    const root = landingPageRef.current;

    if (!root) {
      return undefined;
    }

    const revealElements = Array.from(
      root.querySelectorAll("[data-landing-reveal]"),
    );

    const prefersReducedMotion = window.matchMedia
      ? window.matchMedia("(prefers-reduced-motion: reduce)").matches
      : false;

    if (!("IntersectionObserver" in window) || prefersReducedMotion) {
      revealAll(revealElements);
      return undefined;
    }

    root.classList.add("landing-reveal-ready");

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) {
            return;
          }

          entry.target.classList.add("is-revealed");
          observer.unobserve(entry.target);
        });
      },
      {
        rootMargin: "0px 0px -10% 0px",
        threshold: 0.16,
      },
    );

    revealElements.forEach((element) => {
      observer.observe(element);
    });

    return () => {
      observer.disconnect();
      root.classList.remove("landing-reveal-ready");
    };
  }, []);

  return landingPageRef;
}
