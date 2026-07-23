import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import DashboardIcon from "../common/DashboardIcon";
import ProductChangeModal from "../common/ProductChangeModal";
import { getApiErrorMessage } from "../../utils/apiError";
import {
  getNotifications,
  markNotificationRead,
  prepareDailyNotifications,
} from "../../api/notificationApi";

const POLL_INTERVAL_MS = 60000;

const TYPE_ICON = {
  PAYMENT_DUE: "calendar",
  MISSED_PAYMENT: "info",
  AMOUNT_CHANGE: "repeat",
  BACKFILL_RECALCULATED: "chart",
  UNLINKED_CARD_SUSPECTED: "card",
  INQUIRY_ANSWERED: "check",
  FINANCIAL_PRODUCT_CHANGED: "sparkle",
  CALENDAR_REMINDER: "calendar",
  CONTENT_UPDATED: "sparkle",
};

const formatRelativeTime = (value) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const diffMin = Math.floor((Date.now() - date.getTime()) / 60000);
  if (diffMin < 1) return "방금 전";
  if (diffMin < 60) return `${diffMin}분 전`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 7) return `${diffDay}일 전`;
  return `${date.getFullYear()}. ${date.getMonth() + 1}. ${date.getDate()}.`;
};

function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [changeNotificationId, setChangeNotificationId] = useState(null);
  const rootRef = useRef(null);
  const navigate = useNavigate();

  const applyNotificationData = useCallback((data) => {
    setItems(data?.items || []);
    setUnreadCount(data?.unreadCount || 0);
    setError("");
  }, []);

  const load = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await getNotifications({ size: 20 });
      applyNotificationData(data);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "알림을 불러오지 못했어요."));
    } finally {
      setIsLoading(false);
    }
  }, [applyNotificationData]);

  const prepareAndLoad = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await prepareDailyNotifications({ size: 20 });
      applyNotificationData(data);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "알림을 불러오지 못했어요."));
    } finally {
      setIsLoading(false);
    }
  }, [applyNotificationData]);

  useEffect(() => {
    const run = () => prepareAndLoad();
    run();
    const timer = window.setInterval(load, POLL_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [load, prepareAndLoad]);

  useEffect(() => {
    if (!open) return undefined;
    const handleClickOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  const toggleOpen = () => {
    const next = !open;
    setOpen(next);
    if (next) prepareAndLoad();
  };

  const handleItemClick = async (item) => {
    if (!item.read) {
      setItems((prev) =>
        prev.map((it) => (it.id === item.id ? { ...it, read: true } : it)),
      );
      setUnreadCount((count) => Math.max(0, count - 1));
      try {
        await markNotificationRead(item.id);
      } catch {
        // 읽음 처리 실패는 다음 새로고침에서 다시 반영된다.
      }
    }
    setOpen(false);
    if (item.type === "FINANCIAL_PRODUCT_CHANGED") {
      // 화면 이동 대신 변경 내역 팝업을 띄운다.
      setChangeNotificationId(item.id);
    } else if (item.fixedExpenseId) {
      navigate("/dashboard/fixed-expenses");
    } else if (item.customerInquiryId) {
      navigate(
        `/dashboard/customer-service?tab=myList&inquiryId=${item.customerInquiryId}`,
      );
    } else if (item.type === "CALENDAR_REMINDER") {
      navigate("/dashboard/calendar");
    }
  };

  return (
    <div className="dash-notif" ref={rootRef}>
      <button
        type="button"
        className="dash-notif-trigger"
        onClick={toggleOpen}
        aria-label="알림"
        aria-expanded={open}
      >
        <DashboardIcon name="bell" size={19} />
        {unreadCount > 0 && (
          <span className="dash-notif-badge">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>
      {open && (
        <div className="dash-notif-panel" role="dialog" aria-label="알림 목록">
          <div className="dash-notif-panel-head">
            <strong>알림</strong>
            {isLoading && (
              <span className="dash-notif-loading">불러오는 중...</span>
            )}
          </div>
          {error && <div className="dash-notif-error">{error}</div>}
          {!error && !isLoading && items.length === 0 && (
            <div className="dash-notif-empty">새 알림이 없어요.</div>
          )}
          <ul className="dash-notif-list">
            {items.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  className={`dash-notif-item${item.read ? "" : " unread"}`}
                  onClick={() => handleItemClick(item)}
                >
                  <span className="dash-notif-item-icon">
                    <DashboardIcon
                      name={TYPE_ICON[item.type] || "info"}
                      size={16}
                    />
                  </span>
                  <span className="dash-notif-item-body">
                    <span className="dash-notif-item-message">
                      {item.message}
                    </span>
                    <span className="dash-notif-item-time">
                      {formatRelativeTime(item.createdAt)}
                    </span>
                  </span>
                  {!item.read && <span className="dash-notif-item-dot" />}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
      {changeNotificationId && (
        <ProductChangeModal
          notificationId={changeNotificationId}
          onClose={() => setChangeNotificationId(null)}
        />
      )}
    </div>
  );
}

export default NotificationBell;
