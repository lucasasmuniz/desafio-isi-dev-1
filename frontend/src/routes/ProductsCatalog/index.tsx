import PageHeader from '../../components/PageHeader';
import productsIcon from '../../assets/shopping-bag.svg';
import ProductFilterBar from '../../components/ProductFilterBar';
import ProductTable from '../../components/ProductTable';
import type { ProductDiscountDTO } from "../../models/product";
import * as productService from '../../service/product-service';
import './styles.css';
import { useEffect, useState } from 'react';
import ButtonPrimary from '../../components/ButtonPrimary';


type QueryParams = {
  minPrice: string,
  maxPrice: string,
  searchText: string,
  page: number,
  hasDiscount: boolean
}

export default function ProductsCatalog() {
  const [isLastPage, setIsLastPage] = useState(false);

  const [products, setProducts] = useState<ProductDiscountDTO[]>([]);

  const [queryParams, setQueryParams] = useState<QueryParams>({
    minPrice: '',
    maxPrice: '',
    searchText: '',
    page: 0,
    hasDiscount: false
  })

  useEffect(() => {
    productService.findPageRequest(queryParams.minPrice, queryParams.maxPrice, queryParams.searchText, 6, "name", queryParams.page, queryParams.hasDiscount)
      .then((response) => {
        const nextPage = response.data.content
        setProducts(products.concat(nextPage));
        setIsLastPage(response.data.last);
      })
  }, [queryParams]);

  function handlerFiltering(minPrice: string, maxPrice: string, searchText: string, hasDiscount:boolean) {
    setProducts([]);
    setQueryParams({ ...queryParams, searchText, maxPrice, minPrice, page: 0 ,hasDiscount})
  }

  function handleNextPageClick() {
    setQueryParams({ ...queryParams, page: queryParams.page + 1 })
  }

  function handleResetParams() {
    setProducts([]);
    setQueryParams({ ...queryParams, page: 0 })
  }

  return (
    <>
      <div>
        <PageHeader
          title="Produtos"
          icon={productsIcon}
        />
        <ProductFilterBar onFiltering={handlerFiltering} />
      </div>
      <div>
        <table className="product-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Descrição</th>
              <th>Preço</th>
              <th>Estoque</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {
              products.map(product => [
                <ProductTable product={product} key={product.id} onClick={handleResetParams} />
              ])
            }
          </tbody>
        </table>
      </div>
      {
        !isLastPage &&
        <div onClick={handleNextPageClick} className='catalog-btn'>
          <ButtonPrimary text={"Próxima página"} />
        </div>
      }

    </>

  );
};