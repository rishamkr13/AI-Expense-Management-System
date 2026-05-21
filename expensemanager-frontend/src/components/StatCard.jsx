const StatCard = ({ title, value, subtitle, icon }) => {
  return (
    <div className="stat-card">
      <div className="stat-icon">{icon}</div>

      <div>
        <p className="stat-title">{title}</p>
        <h2 className="stat-value">{value}</h2>
        {subtitle && <p className="stat-subtitle">{subtitle}</p>}
      </div>
    </div>
  );
};

export default StatCard;