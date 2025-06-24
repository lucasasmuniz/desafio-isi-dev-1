import PageHeader from '../../components/PageHeader';
import productsIcon from '../../assets/shopping-bag.svg';

export default function ProductsCatalog(){
  return (
      <div>
        <PageHeader 
          title="Produtos" 
          icon={productsIcon} 
          iconAlt="Ícone de sacola de compras"
        />
      </div>
  );
};