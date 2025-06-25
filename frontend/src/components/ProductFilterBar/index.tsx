import { useState } from 'react';
import searchIcon from '../../assets/search.svg';
import ButtonPrimary from '../ButtonPrimary';

import './styles.css';
import { useNavigate } from 'react-router-dom';
import ButtonSecondary from '../ButtonSecondary';
import { parseCurrencyBRL, formatCurrencyInput } from '../../utils/prices';

type Props = {
    onFiltering : Function;
}

export default function ProductFilterBar({onFiltering}: Props){

  const navigate = useNavigate(); 

  const [useFilter, setUseFilter] = useState<boolean>();
  const [text, setText] = useState<string>("");
  const [minPrice, setMinPrice] = useState<string>("")
  const [maxPrice, setMaxPrice] = useState<string>("")

  function handleOnClickFilter(event: any){
    event.preventDefault();
    if(text === "" && minPrice === "" && maxPrice === ""){
      return;
    }
    setUseFilter(true);
    onFiltering(parseCurrencyBRL(minPrice) === "0.00"? "" : parseCurrencyBRL(minPrice), parseCurrencyBRL(maxPrice) === "0.00"? "" : parseCurrencyBRL(maxPrice), text);
  }

  function handleResetFilter(event: any){
    event.preventDefault();
    setUseFilter(false);
    setText("");
    setMinPrice("");
    setMaxPrice("");
    onFiltering("", "", "");
  }

  function handleOnClickNewProduct(event: any){
    event.preventDefault();
    navigate("new")
  }

  function handleInputTextChange(event: any){
    setText(event.target.value);
  }

  function handlePriceChange(event: any) {
      const numberPattern = /[0-9]+/;
      if(!numberPattern.test(event.target.value)){
          return;
      }
    const rawValue = event.target.value;
    const name = event.target.name;

    const numericOnly = rawValue.replace(/\D/g, '');
    const formatted = formatCurrencyInput(numericOnly);

    if (name === 'min') {
      setMinPrice(formatted);
    } else if (name === 'max') {
      setMaxPrice(formatted);
    }
    event.target.value = formatted;
  }

  return (
    <form className="filter-bar mb-30">
        <div className='price-container'>
          <div className='price-filters'>
            <p>Preço Mínimo</p>
            <input name="min" type="text" onChange={handlePriceChange} min="0" value={minPrice} placeholder='R$ 0,00'/>
          </div>
          <div className='price-filters'>
            <p>Preço Máximo</p>
            <input name="max" type="text" onChange={handlePriceChange} min="0" value={maxPrice} placeholder='R$ 999,99'/>
          </div>
            {
              useFilter
                ?
                <div className='mt-auto' onClick={handleResetFilter}> 
                  <ButtonSecondary text={'↻ Limpar filtro'} />
                </div>
                :
                <div className='mt-auto' onClick={handleOnClickFilter}>  
                  <ButtonPrimary text={'Filtrar'} />
                </div>
            }
        </div>
        <div className='right-filter-container'>
          <div className='searchbar mt-auto'>
            <button><img src={searchIcon} alt="Procurar" /></button>
            <input className='input-filters' onChange={handleInputTextChange} type="text" value={text} placeholder='Buscar produto..'/>
          </div>
          <div className='mt-auto' onClick={handleOnClickNewProduct}>
            <ButtonPrimary text={'+ Criar Produto'} />
          </div>
        </div>
    </form>
  );
};