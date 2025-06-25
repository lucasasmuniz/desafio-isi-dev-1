import './styles.css';

interface PageHeaderProps {
  title: string;
  icon: string;
  iconAlt?: string;
}

export default function PageHeader({ title, icon, iconAlt }: PageHeaderProps) {
  return (
    <div className="page-header mb-30">
      <img src={icon} alt={iconAlt || title} className="page-header-icon" />
      <h1 className="page-header-title">{title}</h1>
    </div>
  );
};