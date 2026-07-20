import { inviteFriend } from "../../../constants/mockDashboard";

function InviteFriendCard() {
  return (
    <div className="invite-card">
      <h3>{inviteFriend.title}</h3>
      <p>{inviteFriend.message}</p>
      <button type="button" className="invite-cta">
        {inviteFriend.cta}
      </button>
    </div>
  );
}

export default InviteFriendCard;
