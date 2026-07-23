import { Link } from "react-router-dom";
import "./BrandLogo.css";

const SIZE_CLASS = {
  small: "themoa-brand-logo-small",
  medium: "themoa-brand-logo-medium",
  large: "themoa-brand-logo-large",
};

function BrandLogo({
  to,
  label = "themoa",
  size = "medium",
  variant = "default",
  ariaLabel,
  onClick,
  className = "",
}) {
  const logoSource = {
    default: "/brand/themoa-logo.png",
    auth: "/brand/themoa-logo-auth.png",
  };
  const imageSrc = logoSource[variant] || logoSource.default;
  const content = (
    <>
      <img
        src={imageSrc}
        alt=""
        className="themoa-brand-logo-image"
      />
      {label && (
        <span className="themoa-brand-logo-label">
          {label}
        </span>
      )}
    </>
  );

  const combinedClassName = [
    "themoa-brand-logo",
    SIZE_CLASS[size],
    variant === "auth" ? "themoa-brand-logo-auth" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  if (to) {
    return (
      <Link
        to={to}
        className={combinedClassName}
        aria-label={ariaLabel || `${label}로 이동`}
        onClick={onClick}
      >
        {content}
      </Link>
    );
  }

  return (
    <div className={combinedClassName}>
      {content}
    </div>
  );
}

export default BrandLogo;
