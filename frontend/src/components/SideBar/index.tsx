import './styles.css';

import iconDashboard from '../../assets/home.svg';
import iconProducts from '../../assets/shopping-bag.svg';
import iconReports from '../../assets/file-text.svg';
import logAdmin from '../../assets/settings.svg';
import iconLogOut from '../../assets/log-out.svg';
import NavItem from '../NavItem';
import { useNavigate } from 'react-router-dom';


export default function Sidebar() {
    const navigate = useNavigate();
    function handleReturnProductCatalog() {
        navigate("/products");
    }
    return (
        <aside className="sidebar">
            <div className="sidebar-logo" onClick={handleReturnProductCatalog}>
                <h1 className="sidebar-logo-text">grupo</h1>
                <h1 className="sidebar-logo-letter">a</h1>
            </div>
            <nav className="sidebar-navigation">
                <NavItem icon={iconDashboard} text="Dashboard" />
                <div onClick={handleReturnProductCatalog}>
                    <NavItem icon={iconProducts} text="Produtos" active={true} />
                </div>
                <NavItem icon={iconReports} text="Relatórios" />
                <NavItem icon={logAdmin} text="Administração" />
            </nav>
            <div className='sidebar-footer'>
                <NavItem icon={iconLogOut} text="Sair" />
            </div>
        </aside>
    )
}