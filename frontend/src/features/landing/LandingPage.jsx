import { useAuth } from "../../hooks/useAuth";
import LandingHeader from "./components/LandingHeader";
import HeroSection from "./components/HeroSection";
import ProblemsSection from "./components/ProblemsSection";
import FeaturesSection from "./components/FeaturesSection";
import HowItWorksSection from "./components/HowItWorksSection";
import DashboardPreviewSection from "./components/DashboardPreviewSection";
import TrustSection from "./components/TrustSection";
import FinalCtaSection from "./components/FinalCtaSection";
import LandingFooter from "./components/LandingFooter";
import { useLandingScrollReveal } from "./hooks/useLandingScrollReveal";
import "./LandingPage.css";

const LOGIN_PATH = "/login";
const SIGNUP_PATH = "/signup";
const DASHBOARD_PATH = "/dashboard";

function LandingPage() {
  const landingPageRef = useLandingScrollReveal();
  const { isAuthenticated } = useAuth();
  const startPath = isAuthenticated ? DASHBOARD_PATH : SIGNUP_PATH;
  const startLabel = isAuthenticated ? "내 대시보드로 이동" : "더모아 시작하기";

  const scrollToSection = (event, sectionId) => {
    event.preventDefault();
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <div className="landing-page" ref={landingPageRef}>
      <LandingHeader
        isAuthenticated={isAuthenticated}
        loginPath={LOGIN_PATH}
        signupPath={SIGNUP_PATH}
        dashboardPath={DASHBOARD_PATH}
        onSectionClick={scrollToSection}
      />
      <main className="landing-main">
        <HeroSection startPath={startPath} onSectionClick={scrollToSection} />
        <ProblemsSection />
        <FeaturesSection />
        <HowItWorksSection />
        <DashboardPreviewSection />
        <TrustSection />
        <FinalCtaSection startPath={startPath} startLabel={startLabel} />
      </main>
      <LandingFooter
        loginPath={LOGIN_PATH}
        startPath={startPath}
        onSectionClick={scrollToSection}
      />
    </div>
  );
}

export default LandingPage;
