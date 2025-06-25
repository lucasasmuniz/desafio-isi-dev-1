
import searchIcon from '../../assets/search.svg';

import './styles.css';

export default function ProductFilterBar(){

  return (
    <form className="filter-bar mb-30">
        <div className='price-container'>
          <div className='price-filters'>
            <p>Preço Mínimo</p>
            <input type="number" min="0" placeholder='R$ 0,00'/>
          </div>
          <div className='price-filters'>
            <p>Preço Máximo</p>
            <input type="number" min="0" placeholder='R$ 999,99'/>
          </div>
          <div className='mt-auto'>
            <button className='button-primary'>Filtrar</button>
          </div>
        </div>
        <div className='searchbar-container'>
          <div className='searchbar mt-auto'>
            <button ><img src={searchIcon} alt="Procurar" /></button>
            <input className='input-filters' type="text" placeholder='Buscar produto..'/>
          </div>
          <div className='mt-auto'>
            <button className='button-primary'>+ Criar Produto</button>
          </div>
        </div>
    </form>
  );
};