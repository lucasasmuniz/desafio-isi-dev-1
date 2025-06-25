import PageHeader from '../../components/PageHeader';
import productsIcon from '../../assets/shopping-bag.svg';
import ProductFilterBar from '../../components/ProductFilterBar';
import ProductTable from '../../components/ProductTable';

export default function ProductsCatalog(){

  return (
    <>
      <div>
        <PageHeader
          title="Produtos"
          icon={productsIcon}
        />
        <ProductFilterBar />
      </div>
      <div>
        <ProductTable />
      </div>
    </>
      
  );
};