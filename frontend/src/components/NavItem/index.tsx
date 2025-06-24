import './styles.css';

interface NavItemProps {
  icon: string;   
  text: string;     
  active?: boolean;
}

const NavItem: React.FC<NavItemProps> = ({ icon, text, active = false }) => {
  
  const activeClass = active ? 'nav-item-active' : '';

  return (
    <a href="#" className={`nav-item ${activeClass}`}>
      <img src={icon} alt={text} className="nav-item-icon" />
      <span className="nav-item-text">{text}</span>
    </a>
  );
};

export default NavItem;