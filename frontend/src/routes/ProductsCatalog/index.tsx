import PageHeader from '../../components/PageHeader';
import productsIcon from '../../assets/shopping-bag.svg';
import ProductFilterBar from '../../components/ProductFilterBar';
import ProductTable from '../../components/ProductTable';
import type { ProductDiscountDTO } from "../../models/product";
import * as productService from '../../service/product-service';


import { useEffect, useState } from 'react';

type QueryParams = {
  minPrice: string,
  maxPrice: string,
  searchText: string,
  page: number
}

export default function ProductsCatalog() {
  const [isLastPage, setIsLastPage] = useState(false);

  const [products, setProducts] = useState<ProductDiscountDTO[]>([]);

  const [queryParams, setQueryParams] = useState<QueryParams>({
    minPrice: '',
    maxPrice: '',
    searchText: '',
    page: 0
  })

  useEffect(() => {
    productService.findPageRequest(queryParams.minPrice, queryParams.maxPrice, queryParams.searchText, 12, "name")
      .then((response) => {
        const nextPage = response.data.content
        setProducts(products.concat(nextPage));
        setIsLastPage(response.data.last);
      })
  }, [queryParams]);

  function handlerFiltering(minPrice: string, maxPrice: string, searchText: string) {
    setProducts([]);
    setQueryParams({ ...queryParams, searchText, maxPrice, minPrice, page: 0 })
  }

  function handleNextPageClick() {
    setQueryParams({ ...queryParams, page: queryParams.page + 1 })
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
                  <ProductTable product={product} key={product.id}/>
                ])
              }
          </tbody>
        </table>
      </div>
    </>

  );
};