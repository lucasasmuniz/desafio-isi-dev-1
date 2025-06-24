import './styles.css';

interface NavItemProps {
  icon: string;   
  text: string;     
  active?: boolean;
}

export default function NavItem({icon, text, active}: NavItemProps)   {
  const activeClass = active ? 'nav-item-active' : '';

  return (
    <a href="#" className={`nav-item ${activeClass}`}>
      <img src={icon} alt={text} className="nav-item-icon" />
      <span className="nav-item-text">{text}</span>
    </a>
  );
};